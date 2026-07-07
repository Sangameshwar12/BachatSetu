package in.bachatsetu.backend.payment.domain.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PaymentPageRequestTest {

    @Test
    void acceptsAValidRequest() {
        PaymentPageRequest request = new PaymentPageRequest(0, 20, PaymentSortField.CREATED_AT, SortDirection.ASC);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
        assertThat(request.sortField()).isEqualTo(PaymentSortField.CREATED_AT);
        assertThat(request.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> new PaymentPageRequest(-1, 20, PaymentSortField.AMOUNT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSizeOutsideTheSupportedRange() {
        assertThatThrownBy(() -> new PaymentPageRequest(0, 0, PaymentSortField.AMOUNT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PaymentPageRequest(
                        0, PaymentPageRequest.MAXIMUM_SIZE + 1, PaymentSortField.AMOUNT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingSortFieldOrDirection() {
        assertThatThrownBy(() -> new PaymentPageRequest(0, 20, null, SortDirection.ASC))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PaymentPageRequest(0, 20, PaymentSortField.AMOUNT, null))
                .isInstanceOf(NullPointerException.class);
    }
}
