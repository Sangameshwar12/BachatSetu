package in.bachatsetu.backend.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Static validation of the production Docker/Compose configuration checked into the
 * repository. Docker itself is not available in every environment this test suite runs in
 * (see the persistence integration tests' {@code disabledWithoutDocker}), so rather than
 * actually building an image, this asserts the specific properties a reviewer would
 * otherwise have to check by hand every time one of these files changes: the backend image
 * runs as a non-root user, declares a HEALTHCHECK, and every Compose file that references
 * a secret-bearing environment variable does not hardcode a real-looking secret value.
 *
 * <p>Paths are resolved relative to the backend module's own working directory
 * ({@code services/backend}, Maven Surefire's default), walking up to the repository root
 * for the files that live outside this module.
 */
class DockerConfigurationValidationTest {

    private static final Path BACKEND_MODULE_ROOT = Path.of("").toAbsolutePath();
    private static final Path REPOSITORY_ROOT = BACKEND_MODULE_ROOT.getParent().getParent();

    @Test
    void backendDockerfileRunsAsANonRootUserAndDeclaresAHealthcheck() throws IOException {
        String dockerfile = readFile(BACKEND_MODULE_ROOT.resolve("Dockerfile"));

        assertThat(dockerfile).contains("USER bachatsetu");
        assertThat(dockerfile).doesNotContain("USER root");
        assertThat(dockerfile).contains("HEALTHCHECK");
        assertThat(dockerfile).contains("FROM maven");
    }

    @Test
    void frontendDockerfileRunsAsANonRootUserAndDeclaresAHealthcheck() throws IOException {
        String dockerfile = readFile(REPOSITORY_ROOT.resolve("services/web/Dockerfile"));

        assertThat(dockerfile).contains("USER nextjs");
        assertThat(dockerfile).contains("HEALTHCHECK");
        assertThat(dockerfile).contains(".next/standalone");
    }

    @Test
    void productionComposeFileRequiresEverySecretFromTheEnvironmentRatherThanDefaultingIt() throws IOException {
        String compose = readFile(REPOSITORY_ROOT.resolve("docker-compose.prod.yml"));

        assertThat(compose).contains("DATABASE_PASSWORD:?");
        assertThat(compose).contains("REDIS_PASSWORD:?");
        assertThat(compose).contains("AUTH_JWT_SIGNING_SECRET:?");
        assertThat(compose).doesNotContain("bachatsetu:bachatsetu");
    }

    @Test
    void monitoringServicesRequireASecretAndDoNotPublishAPublicPort() throws IOException {
        String compose = readFile(REPOSITORY_ROOT.resolve("docker-compose.prod.yml"));

        assertThat(compose).contains("GRAFANA_ADMIN_PASSWORD:?");
        assertThat(compose).contains("127.0.0.1:${PROMETHEUS_PORT:-9090}:9090");
        assertThat(compose).contains("127.0.0.1:${GRAFANA_PORT:-3001}:3000");
    }

    @Test
    void developmentComposeFileIsAdditiveToTheExistingBackendOnlyComposeFile() throws IOException {
        assertThat(Files.exists(REPOSITORY_ROOT.resolve("services/backend/docker-compose.yml"))).isTrue();
        assertThat(Files.exists(REPOSITORY_ROOT.resolve("docker-compose.dev.yml"))).isTrue();
    }

    private String readFile(Path path) throws IOException {
        assertThat(Files.exists(path)).as("expected file to exist: %s", path).isTrue();
        return Files.readString(path);
    }
}
