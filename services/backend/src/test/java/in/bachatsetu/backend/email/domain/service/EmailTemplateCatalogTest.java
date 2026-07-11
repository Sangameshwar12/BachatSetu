package in.bachatsetu.backend.email.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.email.domain.model.EmailTemplate;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import org.junit.jupiter.api.Test;

class EmailTemplateCatalogTest {

    private final EmailTemplateCatalog catalog = new EmailTemplateCatalog();

    @Test
    void hasATemplateForEveryCategory() {
        for (EmailTemplateCategory category : EmailTemplateCategory.values()) {
            EmailTemplate template = catalog.templateFor(category);
            assertThat(template.category()).isEqualTo(category);
            assertThat(template.subjectTemplate()).isNotBlank();
            assertThat(template.htmlTemplate()).isNotBlank();
            assertThat(template.textTemplate()).isNotBlank();
        }
    }

    @Test
    void returnsTheSameTemplateInstanceContentOnRepeatedLookups() {
        assertThat(catalog.templateFor(EmailTemplateCategory.WELCOME))
                .isEqualTo(catalog.templateFor(EmailTemplateCategory.WELCOME));
    }
}
