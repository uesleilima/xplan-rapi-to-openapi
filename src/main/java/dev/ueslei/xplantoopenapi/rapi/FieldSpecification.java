package dev.ueslei.xplantoopenapi.rapi;

import java.util.Arrays;

public enum FieldSpecification {
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
