package in.bachatsetu.backend.architecture.validation;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;

@AnalyzeClasses(
        packages = ArchitecturePackages.ROOT,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ForbiddenApiArchitectureTest {

    @ArchTest
    static final ArchRule SYSTEM_OUT_MUST_NOT_BE_USED = noClasses()
            .should().accessField(System.class, "out")
            .because("production diagnostics must use structured logging");

    @ArchTest
    static final ArchRule SYSTEM_ERR_MUST_NOT_BE_USED = noClasses()
            .should().accessField(System.class, "err")
            .because("production diagnostics must use structured logging");

    @ArchTest
    static final ArchRule THREAD_SLEEP_WITH_MILLISECONDS_MUST_NOT_BE_USED = noClasses()
            .should().callMethod(Thread.class, "sleep", long.class)
            .because("blocking sleeps are not an application scheduling mechanism");

    @ArchTest
    static final ArchRule THREAD_SLEEP_WITH_NANOSECONDS_MUST_NOT_BE_USED = noClasses()
            .should().callMethod(Thread.class, "sleep", long.class, int.class)
            .because("blocking sleeps are not an application scheduling mechanism");

    @ArchTest
    static final ArchRule BIG_DECIMAL_MUST_NOT_BE_CONSTRUCTED_FROM_DOUBLE = noClasses()
            .should().callConstructor(BigDecimal.class, double.class)
            .because("binary floating-point values are unsafe for financial amounts");

    @ArchTest
    static final ArchRule JAVA_UTIL_DATE_MUST_NOT_BE_USED = noClasses()
            .that().doNotHaveSimpleName("JwtProviderAdapter")
            .should().dependOnClassesThat().haveFullyQualifiedName("java.util.Date")
            .because("production time values use java.time except at the isolated JJWT API boundary");

    @ArchTest
    static final ArchRule JAVA_SQL_DATE_MUST_NOT_BE_USED = noClasses()
            .should().dependOnClassesThat().haveFullyQualifiedName("java.sql.Date")
            .because("production time values use the java.time API");

    @ArchTest
    static final ArchRule SPRING_FIELD_INJECTION_MUST_NOT_BE_USED = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .because("dependencies must be explicit through constructor injection");

    @ArchTest
    static final ArchRule JAKARTA_FIELD_INJECTION_MUST_NOT_BE_USED = noFields()
            .should().beAnnotatedWith("jakarta.inject.Inject")
            .because("dependencies must be explicit through constructor injection");

    @ArchTest
    static final ArchRule RESOURCE_FIELD_INJECTION_MUST_NOT_BE_USED = noFields()
            .should().beAnnotatedWith("jakarta.annotation.Resource")
            .because("dependencies must be explicit through constructor injection");
}
