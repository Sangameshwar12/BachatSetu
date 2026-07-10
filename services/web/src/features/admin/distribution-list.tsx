import { Progress } from "@/components/ui/progress";

interface DistributionListProps {
  entries: { key: string; count: number }[];
}

/** Renders a category -> count distribution as labeled percentage bars, reusing the shared Progress primitive. */
export function DistributionList({ entries }: DistributionListProps) {
  const total = entries.reduce((sum, entry) => sum + entry.count, 0);

  if (entries.length === 0 || total === 0) {
    return <p className="text-sm text-muted-foreground">No data yet.</p>;
  }

  return (
    <div className="flex flex-col gap-3">
      {entries.map((entry) => {
        const percent = Math.round((entry.count / total) * 100);
        return (
          <div key={entry.key} className="flex flex-col gap-1">
            <div className="flex items-center justify-between text-xs">
              <span className="font-medium text-foreground">{entry.key}</span>
              <span className="text-muted-foreground">
                {entry.count} ({percent}%)
              </span>
            </div>
            <Progress value={percent} />
          </div>
        );
      })}
    </div>
  );
}
