package dev.ueslei.xplantoopenapi.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.ueslei.xplantoopenapi.config.XplanRapiProperties;
import java.io.IOException;
import java.net.URI;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.AuthenticationException;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(XplanRapiProperties.class)
public class XplanRapiHttpClient {

    private final XplanRapiProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public String fetchXplanDocument(String apiUri) throws IOException, ParseException, AuthenticationException {
        return withUserSession(apiUri, httpclient -> {
            URI uri = URI.create(apiUri);
            HttpOptions httpOptions = new HttpOptions(uri);
            httpOptions.setHeader(HttpHeaders.ACCEPT, "*/*");

            HttpClientResponseHandler<String> responseHandler = response -> {
                int status = response.getCode();
                if (status >= HttpStatus.SC_SUCCESS && status < HttpStatus.SC_REDIRECTION) {
                    HttpEntity entity = response.getEntity();
                    try {
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } catch (ParseException ex) {
                        throw new ClientProtocolException(ex);
                    }
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };
            try {
                return httpclient.execute(httpOptions, responseHandler);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <T> T withUserSession(String apiUri, Function<CloseableHttpClient, T> request)
        throws IOException, ParseException, AuthenticationException {
        var authProperties = properties.getAuthentication();
        URI loginUri = URI.create(apiUri).resolve(authProperties.getLoginPath());

        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(loginUri.getHost(), -1),
            new UsernamePasswordCredentials(authProperties.getUsername(), authProperties.getPassword().toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
            .setDefaultCredentialsProvider(credsProvider)
            .build()) {

            HttpGet httpGet = new HttpGet(loginUri);
            httpGet.setHeader(authProperties.getApiKeyName(), authProperties.getApiKeyValue());

            CloseableHttpResponse response = httpclient.execute(httpGet);

            if (response.getCode() != HttpStatus.SC_SUCCESS) {
                throw new AuthenticationException("Authentication failure");
            }

            var loginResponseBody = EntityUtils.toString(response.getEntity());
            var loginResponseNode = mapper.readValue(loginResponseBody, ObjectNode.class);
            if (loginResponseNode.has("entity_name")) {
                log.info("Accessing resources with {} credentials.", loginResponseNode.get("entity_name"));
            }

            return request.apply(httpclient);
        }
    }

}
