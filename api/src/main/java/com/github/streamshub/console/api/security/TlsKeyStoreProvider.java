package com.github.streamshub.console.api.security;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.config.ConsoleConfig;

import io.quarkus.tls.runtime.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.runtime.KeyStoreProvider;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemKeyCertOptions;

@ApplicationScoped
public class TlsKeyStoreProvider implements KeyStoreProvider {

    @Inject
    ConsoleConfig consoleConfig;

    @Override
    public KeyStoreAndKeyCertOptions getKeyStore(Vertx vertx) {
        return Optional.ofNullable(consoleConfig.getTls())
            .map(tlsConfig -> {
                try {
                    var options = new PemKeyCertOptions()
                            .addCertValue(Buffer.buffer(tlsConfig.getCertificate().get()))
                            .addKeyValue(Buffer.buffer(tlsConfig.getKey().get()));
                    var keyStore = options.loadKeyStore(vertx);
                    return new KeyStoreAndKeyCertOptions(keyStore, options);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .orElse(null);
    }
}
