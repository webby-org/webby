package org.webby.core;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import sun.security.x509.X500Name;
import sun.security.tools.keytool.CertAndKeyGen;

final class TlsTestUtils {
    private static final char[] PASSWORD = "changeit".toCharArray();

    private TlsTestUtils() {
    }

    static SslBundle selfSignedBundle() throws Exception {
        CertAndKeyGen generator = new CertAndKeyGen("RSA", "SHA256withRSA");
        generator.generate(2048);
        X500Name owner = new X500Name("CN=webby-test");
        long validity = 24L * 60 * 60; // one day
        X509Certificate certificate = generator.getSelfCertificate(owner, validity);
        KeyPair keyPair = new KeyPair(generator.getPublicKey(), generator.getPrivateKey());

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry("server", keyPair.getPrivate(), PASSWORD, new java.security.cert.Certificate[]{certificate});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, PASSWORD);
        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(kmf.getKeyManagers(), null, null);

        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("server", certificate);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        SSLContext clientContext = SSLContext.getInstance("TLS");
        clientContext.init(null, tmf.getTrustManagers(), null);

        return new SslBundle(serverContext, clientContext);
    }

    record SslBundle(SSLContext serverContext, SSLContext clientContext) {
    }
}
