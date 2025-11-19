package org.webby.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.CharsetUtil;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.SSLContext;

import org.webby.core.*;

/**
 * Netty-based server that speaks the same {@link org.webby.core.Request}/ {@link Response} protocol as {@code webby-core}.
 */
public final class NettyServer implements AbstractServer {
    private final int port;
    private RequestHandler requestHandler;
    private MiddlewareNode middlewareChain;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private SslContext sslContext;

    /**
     * Creates a new Netty server bound to the supplied port.
     *
     * @param port listening port ({@code 0} for auto)
     */
    public NettyServer(int port) {
        this.port = port;
    }

    /**
     * Configures the request handler. Must be called before {@link #start()}.
     */
    public void setRequestHandler(RequestHandler handler) {
        throwIfRunning();
        this.requestHandler = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Adds middleware that wraps the terminal handler.
     */
    public void addMiddleware(RequestMiddleware middleware) {
        throwIfRunning();
        Objects.requireNonNull(middleware, "middleware");
        middlewareChain = MiddlewareNode.append(middlewareChain, middleware);
    }

    /**
     * Enables TLS using the provided {@link SSLContext}.
     */
    public void enableTls(SSLContext context) {
        throwIfRunning();
        Objects.requireNonNull(context, "context");
        this.sslContext = new JdkSslContext(context, true, ClientAuth.NONE);
    }

    /**
     * Starts the Netty event loop and blocks until {@link #stop()} is invoked.
     */
    public void start() throws InterruptedException {
        RequestHandler handler = requestHandler;
        if (handler == null) {
            throw new IllegalStateException("Request handler must be configured before starting");
        }
        RequestHandler finalHandler = middlewareChain == null ? handler : middlewareChain.wrap(handler);
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            if (sslContext != null) {
                                ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
                            }
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(1_048_576));
                            ch.pipeline().addLast(new NettyRequestHandler(finalHandler));
                        }
                    });
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            serverChannel.closeFuture().sync();
        } finally {
            stop();
        }
    }

    /**
     * Stops the server shutting down all event loops.
     */
    public synchronized void stop() {
        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Indicates whether the server channel is currently active.
     */
    public boolean isRunning() {
        Channel channel = serverChannel;
        return channel != null && channel.isActive();
    }

    /**
     * Exposes the bound port (useful when {@code 0} was provided).
     */
    public int port() {
        Channel channel = serverChannel;
        if (channel == null || channel.localAddress() == null) {
            return port;
        }
        return ((java.net.InetSocketAddress) channel.localAddress()).getPort();
    }

    private void throwIfRunning() {
        if (isRunning()) {
            throw new IllegalStateException("Server is running");
        }
    }

    private static final class MiddlewareNode {
        private final RequestMiddleware middleware;
        private final MiddlewareNode next;

        private MiddlewareNode(RequestMiddleware middleware, MiddlewareNode next) {
            this.middleware = middleware;
            this.next = next;
        }

        static MiddlewareNode append(MiddlewareNode chain, RequestMiddleware middleware) {
            if (chain == null) {
                return new MiddlewareNode(middleware, null);
            }
            return new MiddlewareNode(chain.middleware, append(chain.next, middleware));
        }

        RequestHandler wrap(RequestHandler terminal) {
            RequestHandler nextHandler = next == null ? terminal : next.wrap(terminal);
            return request -> middleware.handle(request, nextHandler);
        }
    }

    private static final class NettyRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private final RequestHandler handler;

        NettyRequestHandler(RequestHandler handler) {
            this.handler = handler;
        }

        @Override
        protected void channelRead0(io.netty.channel.ChannelHandlerContext ctx, FullHttpRequest msg) {
            Response response;
            try {
                response = handleRequest(msg);
            } catch (Exception ex) {
                response = Response.text(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
            }
            writeResponse(ctx, response, msg);
        }

        private Response handleRequest(FullHttpRequest httpRequest) {
            HttpMethod method = HttpMethod.fromToken(httpRequest.method().name());
            if (method == null) {
                return Response.text(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
            }
            Map<String, String> headers = new LinkedHashMap<>();
            httpRequest.headers().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
            byte[] body = ByteBufUtil.getBytes(httpRequest.content());
            org.webby.core.Request request = new org.webby.core.Request(
                    method,
                    httpRequest.uri(),
                    httpRequest.protocolVersion().text(),
                    headers,
                    body);
            Response result = handler.handle(request);
            return Objects.requireNonNullElseGet(result, () -> Response.text(HttpStatus.NO_CONTENT, ""));
        }

        private static void writeResponse(
                io.netty.channel.ChannelHandlerContext ctx, Response response, FullHttpRequest request) {
            byte[] body = response.body();
            FullHttpResponse nettyResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode()), ctx.alloc().buffer(body.length));
            nettyResponse.content().writeBytes(body);
            response.headers().forEach(nettyResponse.headers()::set);
            if (!response.headers().containsKey(HttpHeaderNames.CONTENT_TYPE.toString())) {
                nettyResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            }
            nettyResponse
                    .headers()
                    .set(HttpHeaderNames.CONTENT_LENGTH, nettyResponse.content().readableBytes());
            nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            io.netty.channel.ChannelFuture future = ctx.writeAndFlush(nettyResponse);
            future.addListener(channelFuture -> ctx.close());
        }
    }
}
