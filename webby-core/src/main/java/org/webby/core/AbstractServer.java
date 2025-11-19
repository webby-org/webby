package org.webby.core;

import javax.net.ssl.SSLContext;
import java.io.Closeable;

public interface AbstractServer extends Closeable {
    void setRequestHandler(RequestHandler handler);
    void addMiddleware(RequestMiddleware middleware);
    void enableTls(SSLContext sslContext);
    void start() throws Exception;
    boolean isRunning();
    int port();
}
