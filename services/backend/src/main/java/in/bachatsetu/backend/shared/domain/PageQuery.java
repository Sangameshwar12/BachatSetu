package in.bachatsetu.backend.shared.domain;

/** Plain pagination request, for listings with no filters. */
public record PageQuery(int page, int size) {

    public static final int MAXIMUM_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAXIMUM_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAXIMUM_SIZE);
        }
    }
}
