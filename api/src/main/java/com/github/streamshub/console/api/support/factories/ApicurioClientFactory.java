package com.github.streamshub.console.api.support.factories;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.streamshub.console.api.support.AuthenticationSupport;
import com.github.streamshub.console.api.support.TrustStoreSupport;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.SchemaRegistryConfig;
import com.microsoft.kiota.RequestAdapter;
import com.microsoft.kiota.RequestInformation;

import io.apicurio.registry.resolver.client.RegistryClientFacade;
import io.apicurio.registry.resolver.client.RegistryClientFacadeImpl;
import io.apicurio.registry.resolver.client.RegistryClientFacadeImpl_v2;
import io.kiota.http.jdk.JDKRequestAdapter;
import io.kiota.serialization.json.JsonParseNode;
import io.kiota.serialization.json.JsonParseNodeFactory;

@ApplicationScoped
public class ApicurioClientFactory {

    @Inject
    ObjectMapper mapper;

    @Inject
    ConsoleConfig consoleConfig;

    @Produces
    public Map<String, RegistryClientFacade> produceRegistryClients() {
        return consoleConfig.getSchemaRegistries()
            .stream()
            .map(config -> Map.entry(config.getName(), create(config)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public RegistryClientFacade create(SchemaRegistryConfig config) {
        var adapter = createAdapter(config);

        if (config.getUrl().contains("/apis/registry/v2")) {
            var client = new io.apicurio.registry.rest.client.v2.RegistryClient(adapter);
            return new RegistryClientFacadeImpl_v2(client, null);
        }

        var client = new io.apicurio.registry.rest.client.RegistryClient(adapter);
        return new RegistryClientFacadeImpl(client, null);
    }

    private RequestAdapter createAdapter(SchemaRegistryConfig config) {
        var adapter = new AuthenticatingAdapter(
                handleConfiguration(config).build(),
                new ParseNodeFactory(mapper),
                new AuthenticationSupport(config)
        );

        String url = config.getUrl();

        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        adapter.setBaseUrl(url);
        return adapter;
    }

    private static HttpClient.Builder handleConfiguration(SchemaRegistryConfig config) {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        clientBuilder.version(HttpClient.Version.HTTP_1_1);

        CDI.current().select(TrustStoreSupport.class).get()
            .getTlsConfiguration(config, null)
            .ifPresent(tlsConfig -> {
                try {
                    clientBuilder.sslContext(tlsConfig.createSSLContext());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        return clientBuilder;
    }

    private static class AuthenticatingAdapter extends JDKRequestAdapter {
        private final AuthenticationSupport authentication;

        AuthenticatingAdapter(HttpClient client, JsonParseNodeFactory factory, AuthenticationSupport authentication) {
            super(client, factory);
            this.authentication = authentication;
        }

        @Override
        protected HttpRequest getRequestFromRequestInformation(RequestInformation requestInfo) {
            var request = super.getRequestFromRequestInformation(requestInfo);

            return authentication.get()
                    .map(authn -> HttpRequest.newBuilder(request, (k, v) -> true)
                            .header("Authorization", authn)
                            .build())
                    .orElse(request);
        }
    }

    private static class ParseNodeFactory extends JsonParseNodeFactory {
        private final ObjectMapper mapper;

        ParseNodeFactory(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        protected ObjectReader getObjectReader() {
            return mapper.reader();
        }

        @Override
        protected JsonParseNode createJsonParseNode(JsonNode currentNode) {
            return new ParseNode(this, currentNode);
        }
    }

    private static class ParseNode extends JsonParseNode {
        private static final DateTimeFormatter LENIENT_ISO_OFFSET_DATE_TIME =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]X");

        ParseNode(ParseNodeFactory nodeFactory, JsonNode node) {
            super(nodeFactory, node);
        }

        @Override
        public OffsetDateTime getOffsetDateTimeValue() {
            if (currentNode.isTextual()) {
                return OffsetDateTime.parse(currentNode.textValue(), LENIENT_ISO_OFFSET_DATE_TIME);
            }

            return null;
        }
    }
}
