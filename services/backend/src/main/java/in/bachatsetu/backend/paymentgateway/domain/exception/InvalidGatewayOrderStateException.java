package in.bachatsetu.backend.paymentgateway.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class InvalidGatewayOrderStateException extends DomainException {

    public InvalidGatewayOrderStateException(String message) {
        super(message);
    }
}
