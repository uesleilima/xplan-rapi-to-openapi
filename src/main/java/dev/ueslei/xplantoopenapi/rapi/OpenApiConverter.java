package dev.ueslei.xplantoopenapi.rapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class OpenApiConverter {

    public OpenAPI generateOpenApiSpec(String xplanDocument) {
        Document document = Jsoup.parse(xplanDocument);

        var resource = document.select("h2").first().text();
        var resourceId = resource.replaceAll(" ", "");
        var elements = document.body().getElementsByClass("restpattern");
        var paths = new Paths();
        for (var element : elements) {
            var request = element.getElementsByClass("restrequest").get(0);
            var pathParts = request.text().split(" ");
            var method = HttpMethod.valueOf(pathParts[0]);
            var pathValue = pathParts[1]; // TODO: Format path fields into swagger format.

            var operation = new Operation().operationId(method.name() + resourceId);
            var operationSchema = new Schema().name(resourceId + "Response").type("object");

            var paramsTable = element.getElementsByClass("restparams").get(0);
            var rows = paramsTable.select("tr");
            DocumentSection section = null;
            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);
                Element header = row.select("th").first();
                if (header != null) {
                    section = DocumentSection.fromValue(header.text());
                } else {
                    Elements cols = row.select("td");
                    Optional<Parameter> parameter = Optional.empty();
                    Optional<Schema> schemaProperty = Optional.empty();

                    for (int j = 0; j < cols.size(); j++) {
                        var col = cols.get(j);
                        if (col.hasClass("restparamcomment")) {
                            // TODO: Wrap all fields into an array when text in column is "Array of Objects with fields"
                            System.out.println("\t Comment: " + col.text());
                        } else {
                            String colValue = col.text();
                            var fieldSpec = FieldSpecification.fromIndex(j);

                            switch (section) {
                                case ARGUMENTS:
                                    parameter = Optional.of(populateParameter(parameter, fieldSpec, colValue, "path"));
                                    break;
                                case PARAMETERS:
                                    parameter = Optional.of(populateParameter(parameter, fieldSpec, colValue, "query"));
                                    break;
                                case RESPONSE:
                                    schemaProperty = Optional.of(
                                        populateSchemaProperty(schemaProperty, fieldSpec, colValue));
                                    break;
                            }
                        }
                    }

                    parameter.ifPresent(operation::addParametersItem);
                    schemaProperty.ifPresent(s -> operationSchema.addProperty(s.getName(), s));
                }
            }

            operation.responses(new ApiResponses()
                .addApiResponse("200", new ApiResponse()
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(reprocess(operationSchema))))));

            var existingPathItem = Optional.ofNullable(paths.get(pathValue));
            var pathItem = existingPathItem.orElse(new PathItem());
            pathItem.operation(method, operation);
            paths.addPathItem(pathValue, pathItem);
        }

        OpenAPI openAPI = new OpenAPI()
            .openapi("3.0.3")
            .info(new Info().title(resource + " API").version("1.0.0"))
            .paths(paths);

        return openAPI;
    }

    private Schema reprocess(Schema operationSchema) {
        // TODO: Reprocess schema after finally parsed so we can create array fields
        //  and normalize the required fields list into its parent.
        return operationSchema;
    }

    private Schema populateSchemaProperty(Optional<Schema> schemaPropertyOptional,
        FieldSpecification fieldSpec, String value) {
        Schema schemaProperty = schemaPropertyOptional.orElse(new Schema());
        switch (fieldSpec) {
            case NAME:
                schemaProperty.setName(value);
                break;
            case RESTRICTION:
                if (!value.contains("optional")) {
                    // The required items will be aggregated into its parent schema during `reprocess`.
                    schemaProperty.addRequiredItem(schemaProperty.getName());
                }
                break;
            case TYPE:
                populateType(schemaProperty, value);
                break;
            case DESCRIPTION:
                schemaProperty.setDescription(value);
                break;
        }
        return schemaProperty;
    }

    private Parameter populateParameter(Optional<Parameter> parameterOptional,
        FieldSpecification fieldSpec,
        String value,
        String in) {
        Parameter parameter = parameterOptional.orElse(new Parameter());
        switch (fieldSpec) {
            case NAME:
                parameter.setName(value);
                break;
            case RESTRICTION:
                parameter.setRequired(!value.contains("optional"));
                break;
            case TYPE:
                parameter.setSchema(populateType(new Schema(), value));
                break;
            case DESCRIPTION:
                parameter.setDescription(value);
                break;
        }
        return parameter.in(in);
    }

    private Schema populateType(Schema field, String typeValue) {
        // TODO: Convert XPLAN types into OpenaApi types
        return field.type(typeValue);
    }

}
