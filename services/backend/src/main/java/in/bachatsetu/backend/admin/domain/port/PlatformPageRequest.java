package in.bachatsetu.backend.admin.domain.port;

/** Plain pagination request, for listings with no filters (tenants). */
public record PlatformPageRequest(int page, int size) {

    public static final int MAXIMUM_SIZE = 100;

    public PlatformPageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAXIMUM_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAXIMUM_SIZE);
        }
    }
}
