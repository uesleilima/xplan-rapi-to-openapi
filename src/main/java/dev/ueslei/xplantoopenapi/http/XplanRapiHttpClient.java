package dev.ueslei.xplantoopenapi.http;

import dev.ueslei.xplantoopenapi.config.XplanRapiProperties;
import java.io.IOException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(XplanRapiProperties.class)
public class XplanRapiHttpClient {

    private final XplanRapiProperties properties;

    public String fetchXplanDocument(String apiUri) throws IOException {
        URI uri = URI.create(apiUri);
        var auth = properties.getAuthentication();

        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(uri.getHost(), -1),
            new UsernamePasswordCredentials(auth.getUsername(), auth.getPassword().toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
            .setDefaultCredentialsProvider(credsProvider)
            .build()) {
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
            return httpclient.execute(httpOptions, responseHandler);
        }
    }

}
