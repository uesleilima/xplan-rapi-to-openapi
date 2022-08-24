package dev.ueslei.rapitoopenapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Hello world!
 */
public class App {

    public static void main(String[] args) throws Exception {
//        String xplanDocument = fetchXplanDocs();
        String xplanDocument = Files.readString(java.nio.file.Paths.get(
            "/Users/ueslei/Library/Application Support/JetBrains/IntelliJIdea2022.2/scratches/scratch_9.html"));

        OpenAPI openAPI = generateOpenApiSpec(xplanDocument);
        System.out.println("----------------------------------------");
        System.out.println(openAPI);
    }

    private static String fetchXplanDocs()
        throws IOException, URISyntaxException {
        URI uri = URI.create(System.getenv("XPLAN_URI"));
        String username = System.getenv("XPLAN_USERNAME");
        String password = System.getenv("XPLAN_PASSWORD");

        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(uri.getHost(), -1),
            new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
            .setDefaultCredentialsProvider(credsProvider)
            .build()) {
            HttpOptions httpOptions = new HttpOptions(uri);
            httpOptions.setHeader("Accept", "*/*");

            System.out.println("Executing request " + httpOptions.getMethod() + " " + httpOptions.getUri());

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

    private static OpenAPI generateOpenApiSpec(String xplanDocument) {
        Document document = Jsoup.parse(xplanDocument);
        System.out.println("----------------------------------------");

        var elements = document.body().getElementsByClass("restpattern");
        var paths = new Paths();
        for (var element : elements) {
            var request = element.getElementsByClass("restrequest").get(0);
            var pathParts = request.text().split(" ");
            var method = HttpMethod.valueOf(pathParts[0]);
            var pathValue = pathParts[1];
            System.out.println("Resource: " + method + " " + pathValue);

            var pathItem = new PathItem();
            var operation = new Operation().operationId(method.name() + "Client");

            var paramsTable = element.getElementsByClass("restparams").get(0);
            var rows = paramsTable.select("tr");
            DocumentSection section = null;
            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);
                Element header = row.select("th").first();
                if (header != null) {
                    section = DocumentSection.fromValue(header.text());
                    System.out.println("--- Section: " + section + " ---");
                } else {
                    Elements cols = row.select("td");
                    Optional<Parameter> parameterOpt = Optional.empty();
                    var parameter = new Parameter();

                    for (int j = 0; j < cols.size(); j++) {
                        var col = cols.get(j);
                        if (col.hasClass("restparamcomment")) {
                            System.out.println("\t Comment: " + col.text());
                        } else {
                            parameterOpt = Optional.of(parameter);

                            String colValue = col.text();
                            System.out.println("\t\t" + colValue);

                            var fieldSpec = FieldSpecification.fromIndex(j);
                            switch (fieldSpec) {
                                case NAME:
                                    parameter.setName(colValue);
                                    break;
                                case RESTRICTION:
                                    parameter.setRequired(!"(optional)".contains(colValue));
                                    break;
                                case TYPE:
                                    parameter.setSchema(new Schema().type(colValue));
                                    break;
                                case DESCRIPTION:
                                    parameter.setDescription(colValue);
                                    break;
                            }

                            switch (section) {
                                case ARGUMENTS:
                                    parameter.in("path");
                                    break;
                                case PARAMETERS:
                                    parameter.in("query");
                                    break;
                                case RESPONSE:
                                    parameterOpt = Optional.empty();
                                    break;
                            }
                        }
                    }
                    parameterOpt.ifPresent(operation::addParametersItem);
                }
            }

            pathItem.operation(method, operation);
            paths.addPathItem(pathValue, pathItem);
        }

        OpenAPI openAPI = new OpenAPI().openapi("3.0.3").info(new Info().title("Entity Client API").version("1.0.0"));
        openAPI.paths(paths);

        return openAPI;
    }

    private enum FieldSpecification {
        NAME(0),
        RESTRICTION(1),
        TYPE(2),
        DESCRIPTION(3);

        int index;

        FieldSpecification(int index) {
            this.index = index;
        }

        public static FieldSpecification fromIndex(int index) {
            return Arrays.stream(values()).filter(v -> v.index == index).findFirst().get();
        }
    }

    private enum DocumentSection {
        ARGUMENTS("Resource Arguments"),
        PARAMETERS("Parameters"),
        RESPONSE("Response");

        String value;

        DocumentSection(String value) {
            this.value = value;
        }

        public static DocumentSection fromValue(String value) {
            return Arrays.stream(values()).filter(v -> v.value.equals(value)).findFirst().get();
        }

    }

}
