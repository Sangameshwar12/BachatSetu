package in.bachatsetu.backend.member.application.exception;

/** Raised when a tenant-scoped member profile lookup has no result. */
public final class MemberProfileNotFoundException extends MemberApplicationException {

    public MemberProfileNotFoundException(String message) {
        super(message);
    }
}
