package com.github.streamshub.console.api.support.serdes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.CDI;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.streamshub.console.config.SchemaRegistryConfig;

import io.apicurio.registry.rest.client.impl.ErrorHandler;
import io.apicurio.rest.client.auth.Auth;
import io.apicurio.rest.client.error.RestClientErrorHandler;
import io.apicurio.rest.client.request.Request;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import io.apicurio.rest.client.util.RegistryDateDeserializer;
import io.apicurio.rest.client.util.UriUtil;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;

import static com.github.streamshub.console.support.StringSupport.replaceNonAlphanumeric;
import static java.util.Objects.requireNonNull;

/**
 * Apicurio client based on {@code io.apicurio.rest.client.JdkHttpClient}, but
 * with awareness of Quarkus TLS registry configuration and removing other
 * unused options for mTLS keystores, headers, etc.
 *
 * @see https://github.com/Apicurio/apicurio-common-rest-client/blob/0868773f61e33d40dcac88608aa111e26ab71bc7/rest-client-jdk/src/main/java/io/apicurio/rest/client/JdkHttpClient.java
 */
public class ApicurioClient implements ApicurioHttpClient {

    public static final String INVALID_EMPTY_HTTP_KEY = "";
    private final HttpClient client;
    private final String endpoint;
    private final Auth auth;
    private final RestClientErrorHandler errorHandler;

    private static final ThreadLocal<Map<String, String>> HEADERS = ThreadLocal.withInitial(Collections::emptyMap);

    public ApicurioClient(SchemaRegistryConfig config) {
        String url = config.getUrl();

        if (!url.endsWith("/")) {
            url += "/";
        }

        final HttpClient.Builder httpClientBuilder = handleConfiguration(config);
        this.endpoint = url;
        this.auth = null;
        this.client = httpClientBuilder.build();
        this.errorHandler = new ErrorHandler();
    }

    private HttpClient.Builder handleConfiguration(SchemaRegistryConfig config) {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        clientBuilder.version(HttpClient.Version.HTTP_1_1);

        var tlsConfig = getTlsConfiguration(config.getName());

        if (tlsConfig.isPresent()) {
            try {
                clientBuilder = clientBuilder.sslContext(tlsConfig.get().createSSLContext());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return clientBuilder;
    }

    Optional<TlsConfiguration> getTlsConfiguration(String sourceName) {
        String dotSeparatedSource = "schema.registry." + replaceNonAlphanumeric(sourceName, '.');
        String dashSeparatedSource = "schema-registry-" + replaceNonAlphanumeric(sourceName, '-');
        var tlsRegistry = CDI.current().select(TlsConfigurationRegistry.class).get();
        return tlsRegistry.get(dotSeparatedSource).or(() -> tlsRegistry.get(dashSeparatedSource));
    }

    @Override
    public <T> T sendRequest(Request<T> request) {
        try {
            requireNonNull(request.getOperation(), "Request operation cannot be null");
            requireNonNull(request.getResponseType(), "Response type cannot be null");

            final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(UriUtil.buildURI(endpoint + request.getRequestPath(), request.getQueryParams(), request.getPathParams()));

            //Add current request headers
            HEADERS.get().forEach(requestBuilder::header);
            HEADERS.remove();

            Map<String, String> headers = request.getHeaders();
            if (this.auth != null) {
                //make headers mutable...
                headers = new HashMap<>(headers);
                this.auth.apply(headers);
            }
            headers.forEach(requestBuilder::header);

            switch (request.getOperation()) {
                case GET:
                    requestBuilder.GET();
                    break;
                case PUT:
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofByteArray(request.getData().readAllBytes()));
                    break;
                case POST:
                    if (request.getDataString() != null) {
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(request.getDataString()));
                    } else {
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(request.getData().readAllBytes()));
                    }
                    break;
                case DELETE:
                    requestBuilder.DELETE();
                    break;
                default:
                    throw new IllegalStateException("Operation not allowed");
            }

            return client.sendAsync(requestBuilder.build(), new BodyHandler<>(request.getResponseType(), errorHandler))
                    .join()
                    .body()
                    .get();

        } catch (IOException e) {
            throw errorHandler.parseError(e);
        }
    }

    @Override
    public void setNextRequestHeaders(Map<String, String> headers) {
        HEADERS.set(headers);
    }

    @Override
    public Map<String, String> getHeaders() {
        return HEADERS.get();
    }

    @Override
    public void close() {
        // No-op
    }

    /**
     * From {@code io.apicurio.rest.client.handler.BodyHandler}
     *
     * @see https://github.com/Apicurio/apicurio-common-rest-client/blob/0868773f61e33d40dcac88608aa111e26ab71bc7/rest-client-jdk/src/main/java/io/apicurio/rest/client/handler/BodyHandler.java
     */
    private static class BodyHandler<W> implements HttpResponse.BodyHandler<Supplier<W>> {

        private final TypeReference<W> wClass;
        private final RestClientErrorHandler errorHandler;
        private static final ObjectMapper MAPPER = new ObjectMapper();
        static {
            SimpleModule module = new SimpleModule("Custom date handler");
            module.addDeserializer(Date.class, new RegistryDateDeserializer());
            MAPPER.registerModule(module);
        }

        public BodyHandler(TypeReference<W> wClass, RestClientErrorHandler errorHandler) {
            this.wClass = wClass;
            this.errorHandler = errorHandler;
            MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        @Override
        public HttpResponse.BodySubscriber<Supplier<W>> apply(HttpResponse.ResponseInfo responseInfo) {
            return asJSON(wClass, responseInfo, errorHandler);
        }

        public static <W> HttpResponse.BodySubscriber<Supplier<W>> asJSON(TypeReference<W> targetType, HttpResponse.ResponseInfo responseInfo, RestClientErrorHandler errorHandler) {
            HttpResponse.BodySubscriber<InputStream> upstream = HttpResponse.BodySubscribers.ofInputStream();
            return HttpResponse.BodySubscribers.mapping(
                    upstream,
                    inputStream -> toSupplierOfType(inputStream, targetType, responseInfo, errorHandler));
        }

        @SuppressWarnings("unchecked")
        public static <W> Supplier<W> toSupplierOfType(InputStream body, TypeReference<W> targetType, HttpResponse.ResponseInfo responseInfo, RestClientErrorHandler errorHandler) {
            return () -> {
                try {
                    if (isFailure(responseInfo)) {
                        throw errorHandler.handleErrorResponse(body, responseInfo.statusCode());
                    } else {
                        final String typeName = targetType.getType().getTypeName();
                        if (typeName.contains("InputStream")) {
                            return (W) body;
                        } else if (typeName.contains("Void")) {
                            //Intended null return
                            return null;
                        } else {
                            return MAPPER.readValue(body, targetType);
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
        }

        private static boolean isFailure(HttpResponse.ResponseInfo responseInfo) {
            return responseInfo.statusCode() / 100 != 2;
        }
    }
}
