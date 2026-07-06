package in.bachatsetu.backend.member.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;
import org.junit.jupiter.api.Test;

class MemberNumberGeneratorTest {

    private final MemberNumberGenerator generator = new MemberNumberGenerator();

    @Test
    void generatesAStableFormattedNumberFromTheMemberId() {
        AggregateId memberId = AggregateId.newId();

        MemberNumber number = generator.generate(memberId);

        assertThat(number.value()).startsWith("MB-").hasSize(19);
        assertThat(generator.generate(memberId)).isEqualTo(number);
    }

    @Test
    void rejectsNullMemberId() {
        assertThatThrownBy(() -> generator.generate(null)).isInstanceOf(NullPointerException.class);
    }
}
