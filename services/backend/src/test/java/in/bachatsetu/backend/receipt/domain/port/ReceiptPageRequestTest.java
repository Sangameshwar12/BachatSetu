package in.bachatsetu.backend.receipt.domain.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReceiptPageRequestTest {

    @Test
    void acceptsAValidRequest() {
        ReceiptPageRequest request = new ReceiptPageRequest(0, 20, ReceiptSortField.CREATED_AT, SortDirection.ASC);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
        assertThat(request.sortField()).isEqualTo(ReceiptSortField.CREATED_AT);
        assertThat(request.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> new ReceiptPageRequest(-1, 20, ReceiptSortField.AMOUNT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSizeOutsideTheSupportedRange() {
        assertThatThrownBy(() -> new ReceiptPageRequest(0, 0, ReceiptSortField.AMOUNT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReceiptPageRequest(
                        0, ReceiptPageRequest.MAXIMUM_SIZE + 1, ReceiptSortField.AMOUNT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingSortFieldOrDirection() {
        assertThatThrownBy(() -> new ReceiptPageRequest(0, 20, null, SortDirection.ASC))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ReceiptPageRequest(0, 20, ReceiptSortField.AMOUNT, null))
                .isInstanceOf(NullPointerException.class);
    }
}
