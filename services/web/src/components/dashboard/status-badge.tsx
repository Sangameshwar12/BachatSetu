import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const STATUS_STYLES: Record<string, string> = {
  // Payment / general positive
  VERIFIED: "bg-success/10 text-success",
  DELIVERED: "bg-success/10 text-success",
  SENT: "bg-success/10 text-success",
  ACTIVE: "bg-success/10 text-success",
  COMPLETED: "bg-success/10 text-success",
  GENERATED: "bg-success/10 text-success",
  // In-progress / neutral
  INITIATED: "bg-info/10 text-info",
  PENDING_PROVIDER: "bg-info/10 text-info",
  SENDING: "bg-info/10 text-info",
  QUEUED: "bg-info/10 text-info",
  SCHEDULED: "bg-info/10 text-info",
  OPEN: "bg-info/10 text-info",
  INACTIVE: "bg-muted text-muted-foreground",
  // Attention / warning
  SUSPENDED: "bg-warning/10 text-warning",
  // Negative
  FAILED: "bg-destructive/10 text-destructive",
  CANCELLED: "bg-destructive/10 text-destructive",
  CLOSED: "bg-destructive/10 text-destructive",
  REFUNDED: "bg-destructive/10 text-destructive",
  DISPUTED: "bg-destructive/10 text-destructive",
};

function toLabel(status: string): string {
  return status
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

export function StatusBadge({ status, className }: { status: string; className?: string }) {
  return (
    <Badge variant="outline" className={cn("border-transparent", STATUS_STYLES[status], className)}>
      {toLabel(status)}
    </Badge>
  );
}
