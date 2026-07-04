package in.bachatsetu.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainLayerBoundaryTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java/in/bachatsetu/backend");
    private static final List<String> BUSINESS_MODULES = List.of(
            "auth", "user", "group", "member", "payment", "receipt", "draw", "notification");
    private static final List<String> FORBIDDEN_DOMAIN_REFERENCES = List.of(
            "org.springframework.",
            "jakarta.persistence.",
            "javax.persistence.",
            "@Entity",
            "@Table",
            "@Repository",
            "@Service",
            "@Controller",
            "@RestController");

    @Test
    void everyBusinessModuleHasTheTacticalDomainPackages() {
        for (String module : BUSINESS_MODULES) {
            Path domainRoot = SOURCE_ROOT.resolve(module).resolve("domain");
            assertThat(domainRoot.resolve("model")).isDirectory();
            assertThat(domainRoot.resolve("event")).isDirectory();
            assertThat(domainRoot.resolve("exception")).isDirectory();
            assertThat(domainRoot.resolve("port")).isDirectory();
            assertThat(domainRoot.resolve("factory")).isDirectory();
        }
    }

    @Test
    void domainCodeHasNoFrameworkOrPersistenceDependencies() throws IOException {
        try (var files = Files.walk(SOURCE_ROOT)) {
            List<Path> domainFiles = files
                    .filter(path -> path.toString().contains("domain"))
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path domainFile : domainFiles) {
                String source = Files.readString(domainFile);
                for (String forbiddenReference : FORBIDDEN_DOMAIN_REFERENCES) {
                    assertThat(source)
                            .as("%s must not contain %s", domainFile, forbiddenReference)
                            .doesNotContain(forbiddenReference);
                }
            }
        }
    }
}
