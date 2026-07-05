package in.bachatsetu.backend.security.config;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly typed HTTP authentication and CORS configuration. */
@ConfigurationProperties(prefix = "bachatsetu.authentication.security")
public record AuthenticationSecurityProperties(
        String headerName,
        String bearerPrefix,
        Duration clockSkew,
        int passwordHashStrength,
        List<String> publicEndpoints,
        Cors cors) {

    private static final Duration MAXIMUM_CLOCK_SKEW = Duration.ofMinutes(2);

    public AuthenticationSecurityProperties {
        headerName = requireHeaderName(headerName);
        bearerPrefix = requireBearerPrefix(bearerPrefix);
        Objects.requireNonNull(clockSkew, "security clock skew must not be null");
        if (clockSkew.isNegative() || clockSkew.compareTo(MAXIMUM_CLOCK_SKEW) > 0) {
            throw new IllegalArgumentException("security clock skew must be between zero and two minutes");
        }
        if (passwordHashStrength < 10 || passwordHashStrength > 16) {
            throw new IllegalArgumentException("password hash strength must be between 10 and 16");
        }
        publicEndpoints = List.copyOf(Objects.requireNonNull(publicEndpoints, "public endpoints must not be null"));
        if (publicEndpoints.isEmpty()
                || publicEndpoints.stream().anyMatch(path -> path == null || !path.startsWith("/"))) {
            throw new IllegalArgumentException("public endpoints must contain absolute paths");
        }
        Objects.requireNonNull(cors, "CORS properties must not be null");
    }

    private static String requireHeaderName(String value) {
        Objects.requireNonNull(value, "authentication header name must not be null");
        if (!value.matches("[A-Za-z0-9-]+")) {
            throw new IllegalArgumentException("authentication header name is invalid");
        }
        return value;
    }

    private static String requireBearerPrefix(String value) {
        Objects.requireNonNull(value, "bearer prefix must not be null");
        if (value.isBlank() || !Character.isWhitespace(value.charAt(value.length() - 1))) {
            throw new IllegalArgumentException("bearer prefix must be nonblank and end with whitespace");
        }
        return value;
    }

    /** Browser cross-origin policy owned by the HTTP security boundary. */
    public record Cors(
            List<String> allowedOrigins,
            List<String> allowedMethods,
            List<String> allowedHeaders,
            List<String> exposedHeaders,
            boolean allowCredentials,
            Duration maxAge) {

        public Cors {
            allowedOrigins = requireValues(allowedOrigins, "allowed origins");
            allowedMethods = requireValues(allowedMethods, "allowed methods");
            allowedHeaders = requireValues(allowedHeaders, "allowed headers");
            exposedHeaders = List.copyOf(Objects.requireNonNull(exposedHeaders, "exposed headers must not be null"));
            Objects.requireNonNull(maxAge, "CORS max age must not be null");
            if (maxAge.isNegative()) {
                throw new IllegalArgumentException("CORS max age must not be negative");
            }
            if (allowCredentials && allowedOrigins.contains("*")) {
                throw new IllegalArgumentException("credentialed CORS cannot allow every origin");
            }
        }

        @Override
        public List<String> allowedOrigins() {
            return List.copyOf(allowedOrigins);
        }

        @Override
        public List<String> allowedMethods() {
            return List.copyOf(allowedMethods);
        }

        @Override
        public List<String> allowedHeaders() {
            return List.copyOf(allowedHeaders);
        }

        @Override
        public List<String> exposedHeaders() {
            return List.copyOf(exposedHeaders);
        }

        private static List<String> requireValues(List<String> values, String name) {
            List<String> copy = List.copyOf(Objects.requireNonNull(values, name + " must not be null"));
            if (copy.isEmpty() || copy.stream().anyMatch(String::isBlank)) {
                throw new IllegalArgumentException(name + " must not contain blank values");
            }
            return copy;
        }
    }
}
