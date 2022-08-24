package dev.ueslei.xplantoopenapi.rapi;

import java.util.Arrays;

public enum DocumentSection {
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
