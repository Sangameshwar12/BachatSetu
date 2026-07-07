package in.bachatsetu.backend.member.application.exception;

/** Raised when the acting user is not authorized to perform a member operation. */
public final class MemberAccessDeniedException extends MemberApplicationException {

    public MemberAccessDeniedException(String message) {
        super(message);
    }
}
