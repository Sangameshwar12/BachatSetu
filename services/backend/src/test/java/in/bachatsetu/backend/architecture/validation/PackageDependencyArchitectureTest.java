package in.bachatsetu.backend.architecture.validation;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = ArchitecturePackages.ROOT,
        importOptions = ImportOption.DoNotIncludeTests.class)
class PackageDependencyArchitectureTest {

    @ArchTest
    static final ArchRule TOP_LEVEL_MODULES_MUST_BE_FREE_OF_CYCLES = slices()
            .matching("in.bachatsetu.backend.(*)..")
            .should().beFreeOfCycles()
            .because("module cycles prevent independent evolution and extraction");

    @ArchTest
    static final ArchRule PERSISTENCE_ENTITIES_MUST_NOT_ESCAPE_PERSISTENCE = noClasses()
            .that().resideOutsideOfPackage(ArchitecturePackages.PERSISTENCE)
            .should().dependOnClassesThat().resideInAnyPackage(ArchitecturePackages.PERSISTENCE_ENTITY)
            .because("JPA entities are private persistence representations");
}
