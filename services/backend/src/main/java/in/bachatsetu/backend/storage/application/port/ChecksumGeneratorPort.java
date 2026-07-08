package in.bachatsetu.backend.storage.application.port;

/** Computes a provider-independent integrity checksum for a file's bytes. */
@FunctionalInterface
public interface ChecksumGeneratorPort {

    String generate(byte[] content);
}
