package in.bachatsetu.backend.support.application.port;

import java.util.function.Supplier;

@FunctionalInterface
public interface TransactionPort {

    <T> T execute(Supplier<T> operation);
}
