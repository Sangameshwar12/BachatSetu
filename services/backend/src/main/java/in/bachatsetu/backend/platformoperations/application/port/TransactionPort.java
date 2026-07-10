package in.bachatsetu.backend.platformoperations.application.port;

import java.util.function.Supplier;

@FunctionalInterface
public interface TransactionPort {

    <T> T execute(Supplier<T> operation);
}
