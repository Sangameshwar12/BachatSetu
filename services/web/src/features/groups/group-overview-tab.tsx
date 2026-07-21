import { Card, CardContent } from "@/components/ui/card";
import { StatusBadge } from "@/components/dashboard/status-badge";
import type { SavingsGroupResponse } from "@/types/group";
import { formatDate, formatPaiseAsRupees } from "@/utils/format";

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4 py-2.5 text-sm">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium text-foreground">{value}</span>
    </div>
  );
}

export function GroupOverviewTab({ group }: { group: SavingsGroupResponse }) {
  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardContent className="divide-y divide-border/60">
          <InfoRow label="Group name" value={group.name} />
          <InfoRow label="Group code" value={group.groupCode} />
          <InfoRow label="Organizer" value={group.organizerName ?? "Group organizer"} />
          {group.description && <InfoRow label="Description" value={group.description} />}
          <InfoRow label="Type" value={group.type} />
          <InfoRow label="Status" value={<StatusBadge status={group.status} />} />
          <InfoRow
            label="Contribution"
            value={`${formatPaiseAsRupees(group.contributionAmountPaise)} ${
              group.currencyCode !== "INR" ? group.currencyCode : ""
            }`}
          />
          <InfoRow label="Members" value={`${group.activeMemberCount}/${group.maximumMembers}`} />
          <InfoRow label="Created" value={formatDate(group.createdAt)} />
        </CardContent>
      </Card>
    </div>
  );
}
