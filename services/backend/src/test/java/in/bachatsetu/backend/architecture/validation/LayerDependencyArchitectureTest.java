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
class LayerDependencyArchitectureTest {

    @ArchTest
    static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_OUTER_LAYERS = noClasses()
            .that().resideInAnyPackage(ArchitecturePackages.DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    ArchitecturePackages.APPLICATION,
                    ArchitecturePackages.INTERFACES,
                    ArchitecturePackages.INFRASTRUCTURE,
                    ArchitecturePackages.CONFIGURATION)
            .because("domain code is the framework-independent center of the architecture");

    @ArchTest
    static final ArchRule APPLICATION_MUST_DEPEND_ONLY_ON_DOMAIN_AND_APPLICATION = classes()
            .that().resideInAnyPackage(ArchitecturePackages.APPLICATION)
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    ArchitecturePackages.APPLICATION,
                    ArchitecturePackages.DOMAIN,
                    "java..")
            .allowEmptyShould(true)
            .because("application code may coordinate domain ports but must not know adapters");

    @ArchTest
    static final ArchRule GENERAL_INFRASTRUCTURE_MUST_NOT_DEPEND_ON_APPLICATION_OR_INTERFACES = noClasses()
            .that().resideInAnyPackage(ArchitecturePackages.INFRASTRUCTURE)
            .and().resideOutsideOfPackage(ArchitecturePackages.AUTH_INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAnyPackage(
                    ArchitecturePackages.APPLICATION,
                    ArchitecturePackages.INTERFACES)
            .because("outbound adapters implement domain ports and are not use-case or delivery code");

    @ArchTest
    static final ArchRule AUTH_INFRASTRUCTURE_MAY_DEPEND_ONLY_ON_OWNED_PORTS_AND_DOMAIN = classes()
            .that().resideInAnyPackage(ArchitecturePackages.AUTH_INFRASTRUCTURE)
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    ArchitecturePackages.AUTH_INFRASTRUCTURE,
                    ArchitecturePackages.AUTH_APPLICATION_PORT,
                    "..auth.domain..",
                    "..shared..",
                    "java..",
                    "org.slf4j..",
                    "org.springframework..")
            .because("authentication adapters implement only their owned outbound ports");

    @ArchTest
    static final ArchRule CONTROLLERS_MUST_NOT_DEPEND_ON_DOMAIN_OR_INFRASTRUCTURE = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ArchitecturePackages.DOMAIN,
                    ArchitecturePackages.INFRASTRUCTURE)
            .allowEmptyShould(true)
            .because("controllers may call only the application boundary");

    @ArchTest
    static final ArchRule CONTROLLERS_MUST_NOT_ACCESS_REPOSITORIES = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
            .allowEmptyShould(true)
            .because("repository access belongs behind application use cases");
}
