package in.bachatsetu.backend.draw.domain.model;

public record DrawNumber(int value) {

    public DrawNumber {
        if (value < 1) {
            throw new IllegalArgumentException("draw number must be positive");
        }
    }
}
