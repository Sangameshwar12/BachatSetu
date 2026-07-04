package in.bachatsetu.backend.auth.domain.port;

import in.bachatsetu.backend.auth.domain.model.AuthAccount;
import in.bachatsetu.backend.auth.domain.model.LoginIdentifier;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

public interface AuthAccountRepository {

    Optional<AuthAccount> findById(AggregateId accountId);

    Optional<AuthAccount> findByLoginIdentifier(LoginIdentifier loginIdentifier);

    boolean existsByUserId(AggregateId userId);

    void save(AuthAccount account);
}
