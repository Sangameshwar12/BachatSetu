package in.bachatsetu.backend.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.ReadOnlyJpaRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.NoRepositoryBean;

class PersistenceFoundationTest {

    private static final Path MAIN_SOURCE = Path.of("src/main/java/in/bachatsetu/backend");
    private static final List<String> FORBIDDEN_ENTITIES = List.of(
            "UserEntity.java",
            "GroupEntity.java",
            "PaymentEntity.java",
            "MemberEntity.java");

    @Test
    void baseRepositoriesCannotBeInstantiatedDirectly() {
        assertThat(BaseJpaRepository.class).hasAnnotation(NoRepositoryBean.class);
        assertThat(ReadOnlyJpaRepository.class).hasAnnotation(NoRepositoryBean.class);
    }

    @Test
    void persistenceTypesDoNotLeakIntoDomainPackages() throws IOException {
        try (var paths = Files.walk(MAIN_SOURCE)) {
            List<Path> domainFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().contains("domain"))
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path domainFile : domainFiles) {
                assertThat(Files.readString(domainFile))
                        .as("%s must remain persistence-independent", domainFile)
                        .doesNotContain("in.bachatsetu.backend.infrastructure")
                        .doesNotContain("jakarta.persistence")
                        .doesNotContain("org.springframework.data");
            }
        }
    }

    @Test
    void sprintDoesNotIntroduceBusinessJpaEntities() throws IOException {
        try (var paths = Files.walk(MAIN_SOURCE)) {
            List<String> fileNames = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .toList();

            assertThat(fileNames).doesNotContainAnyElementsOf(FORBIDDEN_ENTITIES);
        }
    }
}
