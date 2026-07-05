package in.bachatsetu.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import in.bachatsetu.backend.shared.domain.Email;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityValueObjectTest {

    private static final String BCRYPT_HASH = "$2b$12$" + "A".repeat(53);
    private static final String OTHER_BCRYPT_HASH = "$2b$12$" + "B".repeat(53);

    @Test
    void typedIdentifiersGenerateParseAndConvertWithoutLosingIdentity() {
        UUID value = UUID.randomUUID();

        UserId userId = UserId.from(value.toString());
        RoleId roleId = RoleId.from(value.toString());
        PermissionId permissionId = PermissionId.from(value.toString());
        RefreshTokenId tokenId = RefreshTokenId.from(value.toString());

        assertThat(userId.value()).isEqualTo(value);
        assertThat(userId.toAggregateId().value()).isEqualTo(value);
        assertThat(userId.toString()).isEqualTo(value.toString());
        assertThat(roleId.toAggregateId().value()).isEqualTo(value);
        assertThat(roleId.toString()).isEqualTo(value.toString());
        assertThat(permissionId.toAggregateId().value()).isEqualTo(value);
        assertThat(permissionId.toString()).isEqualTo(value.toString());
        assertThat(tokenId.toAggregateId().value()).isEqualTo(value);
        assertThat(tokenId.toString()).isEqualTo(value.toString());
        assertThat(List.of(UserId.newId(), RoleId.newId(), PermissionId.newId(), RefreshTokenId.newId()))
                .doesNotContainNull();
    }

    @Test
    void validatesCanonicalEmailAndIndianMobileNumber() {
        Email email = new Email(" Savings.Member@Example.COM ");
        MobileNumber mobile = MobileNumber.of(" +919876543210 ");

        assertThat(email.value()).isEqualTo("savings.member@example.com");
        assertThat(mobile.value()).isEqualTo("+919876543210");
        assertThatIllegalArgumentException().isThrownBy(() -> MobileNumber.of("+915876543210"));
        assertThatIllegalArgumentException().isThrownBy(() -> new Email("not-an-email"));
    }

    @Test
    void acceptsEncodedPasswordHashesAndRejectsPlainText() {
        PasswordHash bcrypt = PasswordHash.encoded(BCRYPT_HASH);
        PasswordHash sameBcrypt = PasswordHash.encoded(BCRYPT_HASH);
        PasswordHash argon2 = PasswordHash.encoded(
                "$argon2id$v=19$m=65536,t=3,p=1$c2FsdA$YWJjZGVm");

        assertThat(bcrypt.value()).isEqualTo(BCRYPT_HASH);
        assertThat(bcrypt).isEqualTo(sameBcrypt).hasSameHashCodeAs(sameBcrypt);
        assertThat(bcrypt).isNotEqualTo(PasswordHash.encoded(OTHER_BCRYPT_HASH));
        assertThat(argon2.value()).startsWith("$argon2id$");
        assertThat(bcrypt.toString()).doesNotContain(BCRYPT_HASH).contains("REDACTED");
        assertThatIllegalArgumentException().isThrownBy(() -> PasswordHash.encoded("plain-text-password"));
        assertThatIllegalArgumentException().isThrownBy(() -> PasswordHash.encoded(" " + BCRYPT_HASH));
    }

    @Test
    void validatesAndRedactsOtpCodes() {
        OtpCode code = OtpCode.of("123456");
        OtpCode sameCode = OtpCode.of("123456");

        assertThat(code.value()).isEqualTo("123456");
        assertThat(code.matches(sameCode)).isTrue();
        assertThat(code.matches(OtpCode.of("654321"))).isFalse();
        assertThat(code).isEqualTo(sameCode).hasSameHashCodeAs(sameCode);
        assertThat(code.toString()).doesNotContain("123456").contains("REDACTED");
        assertThatIllegalArgumentException().isThrownBy(() -> OtpCode.of("12345"));
        assertThatNullPointerException().isThrownBy(() -> code.matches(null));
    }
}
