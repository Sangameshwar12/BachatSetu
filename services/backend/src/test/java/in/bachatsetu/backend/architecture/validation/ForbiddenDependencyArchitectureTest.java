package in.bachatsetu.backend.architecture.validation;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = ArchitecturePackages.ROOT,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ForbiddenDependencyArchitectureTest {

    @ArchTest
    static final ArchRule DOMAIN_MUST_DEPEND_ONLY_ON_DOMAIN_AND_JAVA = classes()
            .that().resideInAnyPackage(ArchitecturePackages.DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    ArchitecturePackages.DOMAIN,
                    "java..")
            .because("domain code must remain portable and framework independent");

    @ArchTest
    static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_SPRING = noClasses()
            .that().resideInAnyPackage(ArchitecturePackages.DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
            .because("domain models must run without the Spring container");

    @ArchTest
    static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_PERSISTENCE_FRAMEWORKS = noClasses()
            .that().resideInAnyPackage(ArchitecturePackages.DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "javax.persistence..",
                    "org.hibernate..")
            .because("domain models must not contain persistence mapping concerns");

    @ArchTest
    static final ArchRule APPLICATION_MUST_NOT_ACCESS_JPA_ENTITIES = noClasses()
            .that().resideInAnyPackage(ArchitecturePackages.APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(ArchitecturePackages.PERSISTENCE_ENTITY)
            .allowEmptyShould(true)
            .because("application use cases exchange domain types rather than persistence entities");
}
