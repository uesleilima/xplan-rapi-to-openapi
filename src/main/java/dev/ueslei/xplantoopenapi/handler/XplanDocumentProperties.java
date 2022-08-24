package dev.ueslei.xplantoopenapi.handler;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "xplan")
public class XplanDocumentProperties {

    private Authentication authentication;

    @Data
    static class Authentication {

        private String username;
        private String password;

    }

}
