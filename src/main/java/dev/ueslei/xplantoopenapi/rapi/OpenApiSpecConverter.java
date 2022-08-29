package dev.ueslei.xplantoopenapi.rapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
public class OpenApiSpecConverter {

    private final OpenApiTypeConverter typeConverter;

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
            var pathValue = formatPath(pathParts[1]);

            var operation = new Operation().operationId(method.name().toLowerCase() + resourceId);
            var operationSchema = new ObjectSchema().name(resourceId + "Response");

            var paramsTable = element.getElementsByClass("restparams").get(0);
            var rows = paramsTable.select("tr");
            DocumentSection section = null;
            boolean isArraySection = false;
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                var header = row.select("th").first();
                if (header != null) {
                    section = DocumentSection.fromValue(header.text());
                } else {
                    var cols = row.select("td");
                    Optional<Parameter> parameter = Optional.empty();
                    Optional<Schema> schemaProperty = Optional.empty();

                    for (int j = 0; j < cols.size(); j++) {
                        var col = cols.get(j);
                        var colValue = col.text();
                        if (col.hasClass("restparamcomment")) {
                            isArraySection = colValue.contains("Array of Object");
                        } else {
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
            aggregateParameterArrays(operation);
            operation.responses(new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("Success")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(reprocessResponse(operationSchema, isArraySection))))));

            var pathItem = Optional.ofNullable(paths.get(pathValue)).orElse(new PathItem());
            pathItem.operation(method, operation);
            paths.addPathItem(pathValue, pathItem);
        }

        var openAPI = new OpenAPI()
            .openapi("3.0.3")
            .info(new Info().title(resource + " API").version("1.0.0"))
            .paths(paths);

        return openAPI;
    }

    /**
     * Format path fields into swagger format.
     *
     * @param pathPart
     * @return
     */
    private String formatPath(String pathPart) {
        return Arrays.stream(pathPart.split("/")).map(s -> {
            if (s.contains(":")) {
                return "{" + s.replace(":", "") + "}";
            }
            return s;
        }).collect(Collectors.joining("/"));
    }

    /**
     * Reprocess schema after finally parsed to adjust response required fields and arrays.
     *
     * @param operationSchema
     * @param isArraySection
     * @return
     */
    private Schema reprocessResponse(Schema operationSchema, boolean isArraySection) {
        aggregateResponseArrays(operationSchema);

        flattenRequiredFields(operationSchema);

        if (isArraySection) {
            return new ArraySchema().items(operationSchema);
        }
        return operationSchema;
    }

    /**
     * Reprocess properties after finally parsed, so we can create aggregated array fields.
     *
     * @param operationSchema
     * @return
     */
    private void aggregateResponseArrays(Schema operationSchema) {
        if (operationSchema.getProperties() == null) {
            return;
        }
        var properties = new ArrayList<Schema>(operationSchema.getProperties().values());
        properties.stream()
            .filter(p -> p.getType().equals("array"))
            .forEach(arrayField -> {
                var arrayFieldName = arrayField.getName();
                var itemsSchema = new ObjectSchema();
                properties.stream().filter(p -> !p.equals(arrayField) && p.getName().startsWith(arrayFieldName))
                    .map(s -> {
                        operationSchema.getProperties().remove(s.getName());
                        var sanitizedName = s.getName().replace(arrayFieldName + "[n].", "");
                        if (!CollectionUtils.isEmpty(s.getRequired())) {
                            s.setRequired(List.of(sanitizedName));
                        }
                        return s.name(sanitizedName);
                    })
                    .forEach(i -> itemsSchema.addProperty(i.getName(), i));
                flattenRequiredFields(itemsSchema);
                arrayField.type("array").setItems(itemsSchema);
            });
    }

    private void aggregateParameterArrays(Operation operation) {
        if (operation.getParameters() == null) {
            return;
        }
        var toClean = new ArrayList<Parameter>();
        operation.getParameters().stream()
            .filter(p -> p.getSchema().getType().equals("array"))
            .forEach(arrayParam -> {
                var arrayFieldName = arrayParam.getName();
                var itemsSchema = new ObjectSchema();
                operation.getParameters().stream()
                    .filter(Objects::nonNull)
                    .filter(p -> !p.equals(arrayParam) && p.getName().startsWith(arrayFieldName))
                    .map(p -> {
                        var sanitizedName = p.getName().replace(arrayFieldName + "[n].", "");
                        toClean.add(p);
                        return new Schema().name(sanitizedName).type(p.getSchema().getType());
                    })
                    .forEach(i -> itemsSchema.addProperty(i.getName(), i));
                arrayParam.getSchema().type("array").setItems(itemsSchema);
            });
        operation.getParameters().removeAll(toClean);
    }

    /**
     * Move required fields from each property to its parent schema.
     *
     * @param schema
     */
    private void flattenRequiredFields(Schema schema) {
        if (schema.getProperties() == null) {
            return;
        }

        List<String> requiredFields = new ArrayList<>();
        for (Object property : schema.getProperties().values()) {
            if (property instanceof Schema propertySchema && !CollectionUtils.isEmpty(propertySchema.getRequired())) {
                requiredFields.addAll(propertySchema.getRequired());
                propertySchema.setRequired(null);
            }
        }

        schema.setRequired(requiredFields);
    }

    private Schema populateSchemaProperty(Optional<Schema> schemaPropertyOptional,
        FieldSpecification fieldSpec, String value) {
        var schemaProperty = schemaPropertyOptional.orElse(new Schema());
        switch (fieldSpec) {
            case NAME:
                schemaProperty.setName(value);
                break;
            case RESTRICTION:
                if (!value.contains("optional")) {
                    // The required items will be aggregated into its parent schema during `reprocessResponse`.
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
        var parameter = parameterOptional.orElse(new Parameter());
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
        return typeConverter.convertFieldType(field, typeValue);
    }

}
