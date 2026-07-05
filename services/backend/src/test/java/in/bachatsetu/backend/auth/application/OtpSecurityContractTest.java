package in.bachatsetu.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.application.event.OtpApplicationEvent;
import in.bachatsetu.backend.auth.application.query.OtpChallengeView;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.OtpVerificationJpaEntity;
import jakarta.persistence.Column;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class OtpSecurityContractTest {

    @Test
    void aggregatePersistenceAndResultsRetainOnlyHashedCredentialMaterial() throws NoSuchFieldException {
        assertThat(Arrays.stream(OtpVerification.class.getDeclaredFields()).map(Field::getType))
                .contains(OtpHash.class)
                .doesNotContain(OtpCode.class);
        assertThat(Arrays.stream(OtpChallengeView.class.getDeclaredFields()).map(Field::getType))
                .doesNotContain(OtpCode.class, OtpHash.class);

        Field hashField = OtpVerificationJpaEntity.class.getDeclaredField("hash");
        assertThat(hashField.getAnnotation(Column.class).name()).isEqualTo("otp_hash");
        assertThat(Arrays.stream(OtpVerificationJpaEntity.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("code", "otpCode");
    }

    @Test
    void applicationEventsCannotExposeOtpCodesOrHashes() {
        assertThat(OtpApplicationEvent.class.getDeclaredMethods())
                .noneMatch(method -> method.getReturnType() == OtpCode.class
                        || method.getReturnType() == OtpHash.class);
    }
}
