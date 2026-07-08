package in.bachatsetu.backend.storage.interfaces.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Sha256ChecksumGeneratorAdapterTest {

    private final Sha256ChecksumGeneratorAdapter adapter = new Sha256ChecksumGeneratorAdapter();

    @Test
    void computesTheKnownSha256DigestOfAnEmptyInput() {
        assertThat(adapter.generate(new byte[0]))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void producesTheSameChecksumForTheSameBytes() {
        byte[] content = "hello checksum".getBytes();

        assertThat(adapter.generate(content)).isEqualTo(adapter.generate(content));
    }

    @Test
    void producesDifferentChecksumsForDifferentBytes() {
        assertThat(adapter.generate("a".getBytes())).isNotEqualTo(adapter.generate("b".getBytes()));
    }
}
