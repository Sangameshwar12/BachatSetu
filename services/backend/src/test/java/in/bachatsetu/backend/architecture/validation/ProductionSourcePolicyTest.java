package in.bachatsetu.backend.architecture.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProductionSourcePolicyTest {

    private static final Path PRODUCTION_SOURCES = Path.of("src", "main", "java");
    private static final Pattern WILDCARD_IMPORT = Pattern.compile(
            "^\\s*import\\s+(?:static\\s+)?[^;]*\\.\\*\\s*;",
            Pattern.MULTILINE);

    @Test
    void productionSourcesMustNotContainWildcardImports() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> sourceFiles = Files.walk(PRODUCTION_SOURCES)) {
            sourceFiles.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> inspect(path, violations));
        }

        assertThat(violations)
                .as("Wildcard imports hide dependencies and are forbidden in production code")
                .isEmpty();
    }

    private static void inspect(Path sourceFile, List<String> violations) {
        try {
            if (WILDCARD_IMPORT.matcher(Files.readString(sourceFile)).find()) {
                violations.add(PRODUCTION_SOURCES.relativize(sourceFile).toString());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to inspect " + sourceFile, exception);
        }
    }
}
