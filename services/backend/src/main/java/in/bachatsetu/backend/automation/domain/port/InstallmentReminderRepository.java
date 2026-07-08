package in.bachatsetu.backend.automation.domain.port;

import in.bachatsetu.backend.automation.domain.model.DueInstallment;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only, cross-tenant access to outstanding contributions, backed by the pre-existing
 * {@code community.installments} table (which predates this module and had no domain or application layer
 * of its own until now). Excludes installments already {@code PAID}, {@code WAIVED}, {@code CANCELLED}, or
 * {@code DISPUTED} in both queries below.
 */
public interface InstallmentReminderRepository {

    /** Outstanding installments due on or after {@code from} and on or before {@code to} (inclusive). */
    List<DueInstallment> findDueBetween(LocalDate from, LocalDate to);

    /** Outstanding installments whose due date is strictly before {@code cutoff}. */
    List<DueInstallment> findOverdueBefore(LocalDate cutoff);
}
