package dev.ueslei.xplantoopenapi.rapi;

import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.stereotype.Component;

@Component
public class OpenApiTypeConverter {

    public Schema convertFieldType(Schema field, String typeValue) {
        String oasType = "string";

        if (typeValue.toLowerCase().contains("integer")) {
            oasType = "integer";
        }

        if (typeValue.toLowerCase().contains("decimal")) {
            oasType = "number";
        }

        if (typeValue.toLowerCase().contains("boolean")) {
            oasType = "boolean";
        }

        if (typeValue.toLowerCase().contains("array")) {
            oasType = "array";
            field.items(new StringSchema());
        }

        if (typeValue.toLowerCase().contains("array of string")) {
            field.items(new StringSchema());
        }

        if (typeValue.toLowerCase().contains("array of object")) {
            field.items(new ObjectSchema());
        }

        if (typeValue.toLowerCase().contains("dictionary")) {
            oasType = "array";
            field.items(new StringSchema());
        }

        if (typeValue.toLowerCase().contains("date")) {
            field.format("date");
        }

        return field.type(oasType);
    }

}
