package dev.ueslei.xplantoopenapi.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ueslei.xplantoopenapi.http.XplanRapiHttpClient;
import dev.ueslei.xplantoopenapi.rapi.OpenApiSpecConverter;
import io.swagger.v3.core.jackson.mixin.MediaTypeMixin;
import io.swagger.v3.core.jackson.mixin.SchemaMixin;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@RequiredArgsConstructor
public class XplanDocumentHandler {

    private final XplanRapiHttpClient client;
    private final OpenApiSpecConverter converter;
    private final ObjectMapper mapper = ObjectMapperFactory.buildStrictGenericObjectMapper()
        .addMixIn(Schema.class, SchemaMixin.class)
        .addMixIn(MediaType.class, MediaTypeMixin.class);

    @ShellMethod("Converts a XPLAN Resourceful API document into an OpenApi 3 specification.")
    public void convert(String uri) throws Exception {
        OpenAPI oasSpec = generateOasSpec(uri);
        String specJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(oasSpec);

        System.out.println("----------------------------------------");
        System.out.println(specJson);
        System.out.println("----------------------------------------");
    }

    @ShellMethod("Generates a XPLAN Resourceful API document into an OpenApi 3 specification json file.")
    public void generate(String uri, String output) throws Exception {
        OpenAPI oasSpec = generateOasSpec(uri);
        mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of(output).toFile(), oasSpec);
    }

    private OpenAPI generateOasSpec(String uri) throws IOException {
        String xplanDocument = client.fetchXplanDocument(uri);
        return converter.generateOpenApiSpec(xplanDocument);
    }


}
