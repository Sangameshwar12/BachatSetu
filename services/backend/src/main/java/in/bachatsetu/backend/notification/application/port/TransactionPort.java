package in.bachatsetu.backend.notification.application.port;

import java.util.function.Supplier;

/** Executes a complete application use case within one transaction boundary. */
@FunctionalInterface
public interface TransactionPort {

    <T> T execute(Supplier<T> operation);
}
