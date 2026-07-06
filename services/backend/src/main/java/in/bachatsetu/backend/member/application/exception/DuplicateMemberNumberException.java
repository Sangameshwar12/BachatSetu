package in.bachatsetu.backend.member.application.exception;

/** Raised when a generated member number is already reserved inside the tenant. */
public final class DuplicateMemberNumberException extends MemberApplicationException {

    public DuplicateMemberNumberException(String message) {
        super(message);
    }
}
