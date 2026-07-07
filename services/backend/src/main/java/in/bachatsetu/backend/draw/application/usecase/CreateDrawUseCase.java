package in.bachatsetu.backend.draw.application.usecase;

import in.bachatsetu.backend.draw.application.command.CreateDrawCommand;
import in.bachatsetu.backend.draw.application.query.DrawResult;

/** Schedules a draw and returns its current state. */
@FunctionalInterface
public interface CreateDrawUseCase {

    DrawResult execute(CreateDrawCommand command);
}
