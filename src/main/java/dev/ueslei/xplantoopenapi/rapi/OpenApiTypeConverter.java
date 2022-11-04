package dev.ueslei.xplantoopenapi.rapi;

import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.stereotype.Component;

@Component
public class OpenApiTypeConverter {

    public Schema convertFieldType(Schema field, String typeValue, boolean isRequestParameter) {
        String oasType = "string";

        if (typeValue.toLowerCase().contains("integer")) {
            oasType = "integer";
        }

        if (typeValue.toLowerCase().contains("decimal")) {
            if (isRequestParameter) {
                oasType = "number";
            } else {
                oasType = "object";
                field.addProperty("_val", new NumberSchema());
                field.addProperty("_type", new StringSchema());
            }
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
            if (isRequestParameter) {
                oasType = "array";
                field.items(new StringSchema());
            } else {
                oasType = "object";
                field.additionalProperties(true);
            }
        }

        if (typeValue.toLowerCase().contains("date")) {
            if (isRequestParameter) {
                field.format("date");
            } else {
                oasType = "object";
                field.addProperty("_val", new DateSchema());
                field.addProperty("_type", new StringSchema());
            }
        }

        return field.type(oasType);
    }

}
