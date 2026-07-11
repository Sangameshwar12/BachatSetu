package in.bachatsetu.backend.email.application.port;

import in.bachatsetu.backend.email.domain.model.EmailMessage;
import in.bachatsetu.backend.email.domain.model.EmailSendResult;

/**
 * Dispatches one already-rendered {@link EmailMessage}. The implementation (a real provider
 * adapter for {@code dev}/{@code prod}, a logging-only adapter for {@code local}/{@code test})
 * decides which provider handles it — callers never know or care. Never throws: retry, provider
 * selection, and failure handling are entirely the implementation's responsibility, so a caller
 * always gets back a result describing whether delivery ultimately succeeded.
 */
@FunctionalInterface
public interface EmailSenderPort {

    EmailSendResult send(EmailMessage message);
}
