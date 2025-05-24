package com.almousleck.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Email email = new Email();
    private final Otp otp = new Otp();
    private final File file = new File();

    @Data
    public static class Jwt {
        private String secret;
        private long expiration;
        private long refreshExpiration;
    }

    @Data
    public static class Cors {
        private String allowedOrigins;
        private String allowedMethods;
        private String allowedHeaders;
    }

    @Data
    public static class Email {
        private String from;
        private String baseUrl;
    }

    @Data
    public static class Otp {
        private int expiration;
    }

    @Data
    public static class File {
        private String uploadDir;
    }
}
