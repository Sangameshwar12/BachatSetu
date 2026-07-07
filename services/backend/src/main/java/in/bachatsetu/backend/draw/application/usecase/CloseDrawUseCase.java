package in.bachatsetu.backend.draw.application.usecase;

import in.bachatsetu.backend.draw.application.command.CloseDrawCommand;
import in.bachatsetu.backend.draw.application.query.DrawResult;

/** Closes an open draw with its winning member. */
@FunctionalInterface
public interface CloseDrawUseCase {

    DrawResult execute(CloseDrawCommand command);
}
