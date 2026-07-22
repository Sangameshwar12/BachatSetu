package in.bachatsetu.backend.email.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.email.domain.model.EmailTemplate;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EmailTemplateCatalogTest {

    /**
     * {@link EmailTemplateCategory#GENERAL_NOTIFICATION} tags already-rendered notification-module
     * dispatches and is never looked up here — see its Javadoc.
     */
    private static final Set<EmailTemplateCategory> CATEGORIES_WITHOUT_A_CATALOG_TEMPLATE =
            EnumSet.of(EmailTemplateCategory.GENERAL_NOTIFICATION);

    private final EmailTemplateCatalog catalog = new EmailTemplateCatalog();

    @Test
    void hasATemplateForEveryCategoryThatUsesTheCatalog() {
        for (EmailTemplateCategory category : EmailTemplateCategory.values()) {
            if (CATEGORIES_WITHOUT_A_CATALOG_TEMPLATE.contains(category)) {
                continue;
            }
            EmailTemplate template = catalog.templateFor(category);
            assertThat(template.category()).isEqualTo(category);
            assertThat(template.subjectTemplate()).isNotBlank();
            assertThat(template.htmlTemplate()).isNotBlank();
            assertThat(template.textTemplate()).isNotBlank();
        }
    }

    @Test
    void hasNoTemplateForGeneralNotification() {
        assertThatThrownBy(() -> catalog.templateFor(EmailTemplateCategory.GENERAL_NOTIFICATION))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void returnsTheSameTemplateInstanceContentOnRepeatedLookups() {
        assertThat(catalog.templateFor(EmailTemplateCategory.WELCOME))
                .isEqualTo(catalog.templateFor(EmailTemplateCategory.WELCOME));
    }
}
