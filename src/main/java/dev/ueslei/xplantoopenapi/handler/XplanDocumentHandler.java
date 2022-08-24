package dev.ueslei.xplantoopenapi.handler;

import dev.ueslei.xplantoopenapi.rapi.OpenApiConverter;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@RequiredArgsConstructor
@EnableConfigurationProperties(XplanDocumentProperties.class)
public class XplanDocumentHandler {

    private final XplanDocumentProperties properties;

    @ShellMethod("Converts a XPLAN Resourceful API document into an OpenApi 3 specification.")
    public void convert(String uri) throws Exception {
        String xplanDocument = fetchXplanDocs(uri);
//        String xplanDocument = Files.readString(Paths.get(
//            "/Users/ueslei/Library/Application Support/JetBrains/IntelliJIdea2022.2/scratches/scratch_9.html"));

        OpenAPI openAPI = new OpenApiConverter().generateOpenApiSpec(xplanDocument);
        System.out.println("----------------------------------------");

        var mapper = ObjectMapperFactory.buildStrictGenericObjectMapper();
        var specJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(openAPI);
        System.out.println(specJson);
    }

    private String fetchXplanDocs(String apiUri) throws IOException, URISyntaxException {
        URI uri = URI.create(apiUri);
        var auth = properties.getAuthentication();

        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(uri.getHost(), -1),
            new UsernamePasswordCredentials(auth.getUsername(), auth.getPassword().toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
            .setDefaultCredentialsProvider(credsProvider)
            .build()) {
            HttpOptions httpOptions = new HttpOptions(uri);
            httpOptions.setHeader("Accept", "*/*");

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
