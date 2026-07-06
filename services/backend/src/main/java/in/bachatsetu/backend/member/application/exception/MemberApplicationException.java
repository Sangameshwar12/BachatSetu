package in.bachatsetu.backend.member.application.exception;

/** Base exception for application-level Member failures. */
public class MemberApplicationException extends RuntimeException {

    public MemberApplicationException(String message) {
        super(message);
    }
}
