package in.bachatsetu.backend.storage.interfaces.rest.adapter;

import in.bachatsetu.backend.storage.application.port.ChecksumGeneratorPort;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Computes a SHA-256 hex digest of a file's bytes using only the JDK — no external checksum library. */
public final class Sha256ChecksumGeneratorAdapter implements ChecksumGeneratorPort {

    private static final String ALGORITHM = "SHA-256";

    @Override
    public String generate(byte[] content) {
        Objects.requireNonNull(content, "content must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(ALGORITHM + " is not available", exception);
        }
    }
}
