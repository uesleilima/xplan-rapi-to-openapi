package dev.ueslei.xplantoopenapi.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ueslei.xplantoopenapi.rapi.OpenApiSpecConverter;
import io.swagger.v3.core.jackson.mixin.MediaTypeMixin;
import io.swagger.v3.core.jackson.mixin.SchemaMixin;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
@RequiredArgsConstructor
public class XplanDocumentHandler {

    private final OpenApiSpecConverter converter;
    private final ObjectMapper mapper = ObjectMapperFactory.buildStrictGenericObjectMapper()
        .addMixIn(Schema.class, SchemaMixin.class)
        .addMixIn(MediaType.class, MediaTypeMixin.class);

    @ShellMethod("Generates a XPLAN Resourceful API document into an OpenApi 3 specification json.")
    public void generate(String uri, @ShellOption(defaultValue = "") String output) throws Exception {
        var oasSpec = converter.generateOpenApiSpec(uri);
        var writer = mapper.writerWithDefaultPrettyPrinter();
        if (output.isEmpty()) {
            var json = writer.writeValueAsString(oasSpec);
            System.out.println(json);
        } else {
            writer.writeValue(Path.of(output).toFile(), oasSpec);
        }
    }


}
