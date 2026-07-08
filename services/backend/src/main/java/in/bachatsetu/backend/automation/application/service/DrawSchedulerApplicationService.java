package in.bachatsetu.backend.automation.application.service;

import in.bachatsetu.backend.automation.application.port.ClockPort;
import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunDrawSchedulerUseCase;
import in.bachatsetu.backend.draw.application.command.ConductDrawCommand;
import in.bachatsetu.backend.draw.application.usecase.ConductDrawUseCase;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.group.domain.port.GroupRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Conducts every {@code SCHEDULED} draw whose scheduled time has arrived, by calling the pre-existing
 * {@link ConductDrawUseCase} for each one — never {@code Draw.open(...)} directly, and never a raw
 * repository save. This orchestrates only; every business rule (scheduled-time ordering, group-owner
 * authorization, the SCHEDULED→OPEN transition itself) still lives where it always has, inside {@code Draw}
 * and {@code ConductDrawApplicationService}.
 *
 * <p>{@link ConductDrawUseCase} requires an {@code actorId} and enforces that it owns the draw's group
 * (mirroring the REST path, where a human organizer conducts their own draw). A scheduled job has no human
 * actor, so it acts on the group's own organizer's behalf — the one identity {@code ConductDrawUseCase}
 * always accepts for this operation — rather than weakening or bypassing that authorization check.
 *
 * <p>Idempotency comes from the query itself: {@link DrawRepository#findDueScheduled} only ever returns
 * draws still in {@code SCHEDULED} status, and a successfully conducted draw becomes {@code OPEN}
 * immediately, so re-running this job (on the next cron tick, or manually) never reprocesses it. A failure
 * conducting one draw is recorded in the returned {@link JobRunResult} and does not stop the remaining
 * draws in the same run from being attempted; this class never logs directly (application code must not
 * depend on a logging framework) — its caller in {@code interfaces.scheduler} logs the failures it reports.
 */
public final class DrawSchedulerApplicationService implements RunDrawSchedulerUseCase {

    private final DrawRepository drawRepository;
    private final GroupRepository groupRepository;
    private final ConductDrawUseCase conductDraw;
    private final ClockPort clock;

    public DrawSchedulerApplicationService(
            DrawRepository drawRepository,
            GroupRepository groupRepository,
            ConductDrawUseCase conductDraw,
            ClockPort clock) {
        this.drawRepository = Objects.requireNonNull(drawRepository, "draw repository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "group repository must not be null");
        this.conductDraw = Objects.requireNonNull(conductDraw, "conduct draw use case must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public JobRunResult execute() {
        List<Draw> dueDraws = drawRepository.findDueScheduled(clock.now());
        int processed = 0;
        List<String> failures = new ArrayList<>();
        for (Draw draw : dueDraws) {
            String failure = conduct(draw);
            if (failure == null) {
                processed++;
            } else {
                failures.add(failure);
            }
        }
        return new JobRunResult(processed, 0, failures);
    }

    private String conduct(Draw draw) {
        try {
            SavingsGroup group = groupRepository.findById(draw.groupId())
                    .orElseThrow(() -> new IllegalStateException("owning group not found for draw " + draw.id()));
            conductDraw.execute(new ConductDrawCommand(draw.tenantId(), draw.id(), group.organizerId()));
            return null;
        } catch (RuntimeException exception) {
            return "Failed to conduct scheduled draw " + draw.id() + ": " + exception.getMessage();
        }
    }
}
