package dev.ueslei.xplantoopenapi.config;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "xplan")
public class XplanRapiProperties {

    @NotNull
    private Authentication authentication;

    @Data
    public static class Authentication {

        @NotEmpty
        private String loginPath;
        @NotEmpty
        private String username;
        @NotEmpty
        private String password;
        @NotEmpty
        private String apiKeyName;
        @NotEmpty
        private String apiKeyValue;

    }

}
