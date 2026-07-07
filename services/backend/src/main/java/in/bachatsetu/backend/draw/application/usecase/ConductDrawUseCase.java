package in.bachatsetu.backend.draw.application.usecase;

import in.bachatsetu.backend.draw.application.command.ConductDrawCommand;
import in.bachatsetu.backend.draw.application.query.DrawResult;

/** Opens a scheduled draw so it can be conducted. */
@FunctionalInterface
public interface ConductDrawUseCase {

    DrawResult execute(ConductDrawCommand command);
}
