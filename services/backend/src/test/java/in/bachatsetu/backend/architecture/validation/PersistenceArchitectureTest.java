package in.bachatsetu.backend.architecture.validation;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;

@AnalyzeClasses(
        packages = ArchitecturePackages.ROOT,
        importOptions = ImportOption.DoNotIncludeTests.class)
class PersistenceArchitectureTest {

    private static final ArchCondition<JavaClass> IMPLEMENT_DOMAIN_REPOSITORY_PORT =
            new ArchCondition<>("implement a repository port declared in a domain package") {
                @Override
                public void check(JavaClass adapter, ConditionEvents events) {
                    if (adapter.getModifiers().contains(JavaModifier.ABSTRACT)) {
                        return;
                    }
                    boolean implementsPort = adapter.getAllRawInterfaces().stream()
                            .anyMatch(type -> type.getPackageName().contains(".domain.")
                                    && type.getPackageName().endsWith(".port")
                                    && type.getSimpleName().endsWith("Repository"));
                    String message = adapter.getName() + " must implement a domain repository port";
                    events.add(new SimpleConditionEvent(adapter, implementsPort, message));
                }
            };

    @ArchTest
    static final ArchRule JPA_ENTITIES_MUST_RESIDE_IN_THE_ENTITY_PACKAGE = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAnyPackage(ArchitecturePackages.PERSISTENCE_ENTITY)
            .because("only the persistence entity package may own JPA entity mappings");

    @ArchTest
    static final ArchRule REPOSITORY_ADAPTERS_MUST_IMPLEMENT_DOMAIN_PORTS = classes()
            .that().haveSimpleNameEndingWith("RepositoryAdapter")
            .should(IMPLEMENT_DOMAIN_REPOSITORY_PORT)
            .because("persistence adapters fulfill inward-facing domain contracts");
}
