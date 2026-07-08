package in.bachatsetu.backend.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.application.command.GenerateOtpCommand;
import in.bachatsetu.backend.auth.application.command.InvalidateOtpCommand;
import in.bachatsetu.backend.auth.application.command.ResendOtpCommand;
import in.bachatsetu.backend.auth.application.command.VerifyOtpCommand;
import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpRejected;
import in.bachatsetu.backend.auth.application.event.OtpRejectionReason;
import in.bachatsetu.backend.auth.application.event.OtpRequested;
import in.bachatsetu.backend.auth.application.event.OtpResent;
import in.bachatsetu.backend.auth.application.event.OtpSent;
import in.bachatsetu.backend.auth.application.event.OtpVerified;
import in.bachatsetu.backend.auth.application.exception.OtpApplicationException;
import in.bachatsetu.backend.auth.application.exception.OtpFailureReason;
import in.bachatsetu.backend.auth.application.event.OtpApplicationEvent;
import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.HashingPort;
import in.bachatsetu.backend.auth.application.port.OtpEventPublisherPort;
import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.auth.application.port.RandomGeneratorPort;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.validation.OtpRequestValidator;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.auth.domain.port.OtpVerificationRepository;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.auth.domain.service.OtpPolicyService;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OtpApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");
    private static final AggregateId ACTOR_ID = AggregateId.newId();
    private static final UserId USER_ID = UserId.newId();
    private static final OtpCode VALID_CODE = OtpCode.of("123456");
    private static final OtpCode INVALID_CODE = OtpCode.of("654321");

    @Test
    void generatesHashesPersistsAndSendsWithoutExposingSecrets() {
        Fixture fixture = new Fixture();

        OtpActionResult result = fixture.generate.generate(
                new GenerateOtpCommand(USER_ID, OtpPurpose.SIGN_IN, ACTOR_ID));

        assertThat(fixture.repository.current.hash()).isEqualTo(FakeHashingPort.VALID_HASH);
        assertThat(fixture.repository.current.expiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(fixture.sender.sentCodes).containsExactly(VALID_CODE);
        assertThat(result.events()).hasExactlyElementsOfTypes(OtpRequested.class, OtpSent.class);
        assertThat(result.challenge().status()).isEqualTo(OtpStatus.PENDING);
        assertThat(result.toString()).doesNotContain(VALID_CODE.value(), FakeHashingPort.VALID_HASH.value());
        assertThatThrownBy(() -> result.events().add(result.events().getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsConcurrentActiveOtpAndReplacesExpiredOtp() {
        Fixture fixture = new Fixture();
        fixture.repository.current = fixture.challenge(NOW, 0);

        assertThatThrownBy(() -> fixture.generate.generate(
                        new GenerateOtpCommand(USER_ID, OtpPurpose.SIGN_IN, ACTOR_ID)))
                .isInstanceOfSatisfying(OtpApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(OtpFailureReason.ACTIVE_OTP_EXISTS));

        fixture.clock.now = NOW.plusSeconds(300);
        OtpActionResult result = fixture.generate.generate(
                new GenerateOtpCommand(USER_ID, OtpPurpose.SIGN_IN, ACTOR_ID));

        assertThat(result.events()).hasExactlyElementsOfTypes(OtpExpired.class, OtpRequested.class, OtpSent.class);
        assertThat(fixture.repository.savedStatuses).contains(OtpStatus.EXPIRED, OtpStatus.PENDING);
    }

    @Test
    void verifiesMatchingOtpAndRejectsFiveIncorrectAttempts() {
        Fixture success = new Fixture();
        success.repository.current = success.challenge(NOW, 0);

        OtpActionResult verified = success.verify.verify(
                new VerifyOtpCommand(USER_ID, OtpPurpose.SIGN_IN, VALID_CODE, ACTOR_ID));

        assertThat(verified.challenge().status()).isEqualTo(OtpStatus.VERIFIED);
        assertThat(verified.events()).singleElement().isInstanceOf(OtpVerified.class);

        Fixture rejected = new Fixture();
        rejected.repository.current = rejected.challenge(NOW, 0);
        for (int attempt = 1; attempt <= OtpVerification.MAXIMUM_VERIFICATION_ATTEMPTS; attempt++) {
            OtpActionResult result = rejected.verify.verify(
                    new VerifyOtpCommand(USER_ID, OtpPurpose.SIGN_IN, INVALID_CODE, ACTOR_ID));
            OtpRejected event = (OtpRejected) result.events().getFirst();
            assertThat(event.reason()).isEqualTo(attempt == OtpVerification.MAXIMUM_VERIFICATION_ATTEMPTS
                    ? OtpRejectionReason.ATTEMPT_LIMIT
                    : OtpRejectionReason.INVALID_CODE);
        }
        assertThat(rejected.repository.current.status()).isEqualTo(OtpStatus.FAILED);
    }

    @Test
    void expiresBeforeHashComparison() {
        Fixture fixture = new Fixture();
        fixture.repository.current = fixture.challenge(NOW, 0);
        fixture.clock.now = NOW.plusSeconds(300);

        OtpActionResult result = fixture.verify.verify(
                new VerifyOtpCommand(USER_ID, OtpPurpose.SIGN_IN, VALID_CODE, ACTOR_ID));

        assertThat(result.challenge().status()).isEqualTo(OtpStatus.EXPIRED);
        assertThat(result.events()).singleElement().isInstanceOf(OtpExpired.class);
        assertThat(fixture.hashing.matchCalls).isZero();
    }

    @Test
    void resendsAtomicallyInvalidatesOldOtpAndTracksExpiry() {
        Fixture fixture = new Fixture();
        OtpVerification current = fixture.challenge(NOW, 0);
        fixture.repository.current = current;
        fixture.clock.now = NOW.plusSeconds(30);

        OtpActionResult result = fixture.resend.resend(
                new ResendOtpCommand(USER_ID, OtpPurpose.SIGN_IN, ACTOR_ID));

        assertThat(current.status()).isEqualTo(OtpStatus.INVALIDATED);
        assertThat(result.challenge().resendCount()).isOne();
        assertThat(result.events()).hasExactlyElementsOfTypes(OtpResent.class, OtpSent.class);
        assertThat(fixture.repository.replacements).isOne();

        Fixture expired = new Fixture();
        expired.repository.current = expired.challenge(NOW, 0);
        expired.clock.now = NOW.plusSeconds(300);
        OtpActionResult expiredResult = expired.resend.resend(
                new ResendOtpCommand(USER_ID, OtpPurpose.SIGN_IN, ACTOR_ID));
        assertThat(expiredResult.events()).hasExactlyElementsOfTypes(OtpExpired.class, OtpResent.class, OtpSent.class);
    }

    @Test
    void enforcesResendLimitAndInvalidatesPendingOtp() {
        Fixture fixture = new Fixture();
        fixture.repository.current = fixture.challenge(NOW, OtpVerification.MAXIMUM_RESENDS);

        assertThatThrownBy(() -> fixture.resend.resend(
                        new ResendOtpCommand(USER_ID, OtpPurpose.SIGN_IN, ACTOR_ID)))
                .isInstanceOfSatisfying(OtpApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(OtpFailureReason.RESEND_LIMIT_REACHED));

        fixture.repository.current = fixture.challenge(NOW, 1);
        OtpActionResult invalidated = fixture.invalidate.invalidate(
                new InvalidateOtpCommand(USER_ID, OtpPurpose.SIGN_IN, ACTOR_ID));
        assertThat(invalidated.challenge().status()).isEqualTo(OtpStatus.INVALIDATED);
        assertThat(invalidated.events()).isEmpty();
    }

    @Test
    void validatesUserAndRequiredOtpStateForEveryCommand() {
        Fixture missingUser = new Fixture(false);
        assertFailure(
                () -> missingUser.generate.generate(
                        new GenerateOtpCommand(USER_ID, OtpPurpose.SIGN_IN, ACTOR_ID)),
                OtpFailureReason.USER_NOT_FOUND);

        Fixture fixture = new Fixture();
        assertFailure(
                () -> fixture.verify.verify(
                        new VerifyOtpCommand(USER_ID, OtpPurpose.SIGN_IN, VALID_CODE, ACTOR_ID)),
                OtpFailureReason.OTP_NOT_FOUND);
        assertFailure(
                () -> fixture.resend.resend(
                        new ResendOtpCommand(USER_ID, OtpPurpose.SIGN_IN, ACTOR_ID)),
                OtpFailureReason.OTP_NOT_FOUND);
        assertFailure(
                () -> fixture.invalidate.invalidate(
                        new InvalidateOtpCommand(USER_ID, OtpPurpose.SIGN_IN, ACTOR_ID)),
                OtpFailureReason.OTP_NOT_FOUND);
    }

    private void assertFailure(ThrowingOperation operation, OtpFailureReason reason) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(OtpApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(reason));
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run();
    }

    private static final class Fixture {

        private final MutableClock clock = new MutableClock();
        private final InMemoryOtpRepository repository = new InMemoryOtpRepository();
        private final FakeHashingPort hashing = new FakeHashingPort();
        private final CapturingSender sender = new CapturingSender();
        private final QueueRandomGenerator random = new QueueRandomGenerator();
        private final CapturingOtpEventPublisher eventPublisher = new CapturingOtpEventPublisher();
        private final OtpRequestValidator validator;
        private final GenerateOtpApplicationService generate;
        private final VerifyOtpApplicationService verify;
        private final ResendOtpApplicationService resend;
        private final InvalidateOtpApplicationService invalidate;

        private Fixture() {
            this(true);
        }

        private Fixture(boolean userExists) {
            UserRepository userRepository = new StubUserRepository(userExists ? user() : null);
            validator = new OtpRequestValidator(userRepository);
            OtpPolicyService policy = new OtpPolicyService();
            generate = new GenerateOtpApplicationService(
                    validator, repository, policy, clock, random, hashing, sender);
            verify = new VerifyOtpApplicationService(validator, repository, clock, hashing, eventPublisher);
            resend = new ResendOtpApplicationService(
                    validator, repository, policy, clock, random, hashing, sender);
            invalidate = new InvalidateOtpApplicationService(validator, repository, clock);
        }

        private OtpVerification challenge(Instant generatedAt, int resendCount) {
            return OtpVerification.rehydrate(
                    AggregateId.newId(),
                    USER_ID,
                    FakeHashingPort.VALID_HASH,
                    OtpPurpose.SIGN_IN,
                    generatedAt,
                    generatedAt.plusSeconds(300),
                    OtpStatus.PENDING,
                    0,
                    resendCount,
                    AuditInfo.createdBy(ACTOR_ID, generatedAt),
                    0);
        }
    }

    private static User user() {
        return User.rehydrate(
                USER_ID,
                new Email("member@example.com"),
                MobileNumber.of("+919876543210"),
                PasswordHash.encoded("$2b$12$" + "A".repeat(53)),
                UserStatus.ACTIVE,
                java.util.Set.of(),
                AuditInfo.createdBy(ACTOR_ID, NOW),
                0);
    }

    private static final class MutableClock implements ClockPort {
        private Instant now = NOW;

        @Override
        public Instant now() {
            return now;
        }
    }

    private static final class QueueRandomGenerator implements RandomGeneratorPort {
        private final ArrayDeque<OtpCode> codes = new ArrayDeque<>(List.of(VALID_CODE, INVALID_CODE));

        @Override
        public OtpCode generateOtp() {
            return codes.removeFirst();
        }
    }

    private static final class FakeHashingPort implements HashingPort {
        private static final OtpHash VALID_HASH = OtpHash.encoded("A".repeat(64));
        private static final OtpHash INVALID_HASH = OtpHash.encoded("B".repeat(64));
        private int matchCalls;

        @Override
        public OtpHash hash(OtpCode code) {
            return code.equals(VALID_CODE) ? VALID_HASH : INVALID_HASH;
        }

        @Override
        public boolean matches(OtpCode candidate, OtpHash hash) {
            matchCalls++;
            return hash(candidate).equals(hash);
        }
    }

    private static final class CapturingSender implements OtpSenderPort {
        private final List<OtpCode> sentCodes = new ArrayList<>();

        @Override
        public void send(UserId userId, MobileNumber mobileNumber, OtpPurpose purpose, OtpCode code) {
            sentCodes.add(code);
        }
    }

    private static final class CapturingOtpEventPublisher implements OtpEventPublisherPort {
        private final List<OtpApplicationEvent> published = new ArrayList<>();

        @Override
        public void publish(OtpApplicationEvent event) {
            published.add(event);
        }
    }

    private static final class InMemoryOtpRepository implements OtpVerificationRepository {
        private OtpVerification current;
        private final List<OtpStatus> savedStatuses = new ArrayList<>();
        private int replacements;

        @Override
        public Optional<OtpVerification> findById(AggregateId verificationId) {
            return Optional.ofNullable(current).filter(value -> value.id().equals(verificationId));
        }

        @Override
        public Optional<OtpVerification> findActive(UserId userId, OtpPurpose purpose) {
            return Optional.ofNullable(current)
                    .filter(value -> value.userId().equals(userId))
                    .filter(value -> value.purpose() == purpose)
                    .filter(value -> value.status() == OtpStatus.PENDING);
        }

        @Override
        public void save(OtpVerification verification) {
            current = verification;
            savedStatuses.add(verification.status());
        }

        @Override
        public void replace(OtpVerification currentVerification, OtpVerification replacement) {
            assertThat(currentVerification.status()).isIn(OtpStatus.INVALIDATED, OtpStatus.EXPIRED);
            current = replacement;
            replacements++;
        }
    }

    private static final class StubUserRepository implements UserRepository {
        private final User user;

        private StubUserRepository(User user) {
            this.user = user;
        }

        @Override
        public Optional<User> findById(UserId userId) {
            return Optional.ofNullable(user).filter(value -> value.userId().equals(userId));
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByMobileNumber(MobileNumber mobileNumber) {
            return Optional.empty();
        }

        @Override
        public void save(User user) {
        }
    }
}
