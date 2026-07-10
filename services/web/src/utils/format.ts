/** Formats a plain number with locale-aware thousands separators, e.g. `12000` -> `12,000`. */
export function formatCompactNumber(value: number): string {
  return new Intl.NumberFormat("en-IN", { notation: "compact", maximumFractionDigits: 1 }).format(value);
}

/** Formats an integer amount of minor units (paise) as a localized rupee amount, e.g. `150000` -> `₹1,500.00`. */
export function formatPaiseAsRupees(paise: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
  }).format(paise / 100);
}

/** Formats an ISO timestamp as a short date, e.g. `10 Jul 2026`. */
export function formatDate(isoTimestamp: string): string {
  return new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", year: "numeric" }).format(
    new Date(isoTimestamp)
  );
}

/** Formats an ISO timestamp as a short date and time, e.g. `10 Jul 2026, 4:30 pm`. */
export function formatDateTime(isoTimestamp: string): string {
  return new Intl.DateTimeFormat("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(isoTimestamp));
}

/** Formats an ISO timestamp relative to now, e.g. `2 hours ago`, `in 3 days`. */
export function formatRelativeTime(isoTimestamp: string): string {
  const diffMs = new Date(isoTimestamp).getTime() - Date.now();
  const diffSeconds = Math.round(diffMs / 1000);
  const divisions: { amount: number; unit: Intl.RelativeTimeFormatUnit }[] = [
    { amount: 60, unit: "seconds" },
    { amount: 60, unit: "minutes" },
    { amount: 24, unit: "hours" },
    { amount: 7, unit: "days" },
    { amount: 4.34524, unit: "weeks" },
    { amount: 12, unit: "months" },
    { amount: Number.POSITIVE_INFINITY, unit: "years" },
  ];

  const formatter = new Intl.RelativeTimeFormat("en-IN", { numeric: "auto" });
  let duration = diffSeconds;
  for (const division of divisions) {
    if (Math.abs(duration) < division.amount) {
      return formatter.format(Math.round(duration), division.unit);
    }
    duration /= division.amount;
  }
  return formatter.format(Math.round(duration), "years");
}
