package in.bachatsetu.backend.user.domain.model;

import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.shared.domain.PhoneNumber;

public record UserContact(Email email, PhoneNumber phoneNumber) {

    public UserContact {
        if (email == null && phoneNumber == null) {
            throw new IllegalArgumentException("at least one contact method is required");
        }
    }
}
