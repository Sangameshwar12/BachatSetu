package in.bachatsetu.backend.notification.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NotificationTemplateCatalogTest {

    @ParameterizedTest
    @EnumSource(NotificationCategory.class)
    void everyCategoryHasARegisteredTemplate(NotificationCategory category) {
        NotificationTemplate template = NotificationTemplateCatalog.templateFor(category);

        assertThat(template.category()).isEqualTo(category);
        assertThat(template.bodyTemplate()).isNotBlank();
    }

    @Test
    void rejectsNullCategory() {
        assertThatThrownBy(() -> NotificationTemplateCatalog.templateFor(null))
                .isInstanceOf(NullPointerException.class);
    }
}
