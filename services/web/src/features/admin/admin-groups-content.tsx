"use client";

import { Gavel, Search } from "lucide-react";
import { useState } from "react";

import { PageContainer } from "@/components/dashboard/page-container";
import { PaginationControls } from "@/components/dashboard/pagination-controls";
import { StatusBadge } from "@/components/dashboard/status-badge";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { useAdminGroups } from "@/hooks/use-admin-groups";
import type { PlatformGroupResponse } from "@/types/admin";
import { formatDate } from "@/utils/format";

const PAGE_SIZE = 20;
const STATUS_OPTIONS = ["ACTIVE", "INACTIVE", "SUSPENDED", "CLOSED"];

function GroupDetailsDialog({ group }: { group: PlatformGroupResponse }) {
  return (
    <Dialog>
      <DialogTrigger render={<Button variant="outline" size="sm" />}>View details</DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{group.name}</DialogTitle>
          <DialogDescription>{group.code}</DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-2 text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">Status</span>
            <StatusBadge status={group.status} />
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Members</span>
            <span>{group.memberCount}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Created</span>
            <span>{formatDate(group.createdAt)}</span>
          </div>
          <div className="flex justify-between gap-4">
            <span className="text-muted-foreground">Tenant</span>
            <span className="truncate font-mono text-xs">{group.tenantId}</span>
          </div>
          <div className="flex justify-between gap-4">
            <span className="text-muted-foreground">Group ID</span>
            <span className="truncate font-mono text-xs">{group.groupId}</span>
          </div>
        </div>
        <DialogFooter showCloseButton />
      </DialogContent>
    </Dialog>
  );
}

export function AdminGroupsContent() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);

  const { data, isPending, isError, error, refetch } = useAdminGroups({
    status,
    page,
    size: PAGE_SIZE,
  });

  const visibleGroups = data?.content.filter(
    (group) =>
      search.trim() === "" ||
      group.name.toLowerCase().includes(search.trim().toLowerCase()) ||
      group.code.toLowerCase().includes(search.trim().toLowerCase())
  );

  return (
    <PageContainer title="Group Management" description="Savings groups across every tenant.">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1 sm:max-w-xs">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Filter this page by name or code"
            className="pl-8"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <Select
          value={status}
          onValueChange={(value) => {
            setPage(0);
            setStatus(value === "ALL" ? undefined : (value as string));
          }}
        >
          <SelectTrigger className="sm:w-48">
            <SelectValue placeholder="All statuses" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All statuses</SelectItem>
            {STATUS_OPTIONS.map((option) => (
              <SelectItem key={option} value={option}>
                {option}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Alert>
        <AlertDescription>
          The groups endpoint has no server-side text search, so the box above only filters the groups already
          loaded on this page. Contribution amount isn&apos;t exposed on this cross-tenant view either — open
          &quot;View details&quot; for what is available.
        </AlertDescription>
      </Alert>

      {isPending ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 6 }, (_, index) => (
            <Skeleton key={index} className="h-12 rounded-lg" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : !visibleGroups || visibleGroups.length === 0 ? (
        <EmptyState icon={Gavel} title="No groups match these filters" description="Try clearing the search or status filter." />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-border/60">
          <table className="w-full text-left text-sm">
            <thead className="bg-muted/40 text-xs text-muted-foreground">
              <tr>
                <th className="px-4 py-2 font-medium">Name</th>
                <th className="px-4 py-2 font-medium">Code</th>
                <th className="px-4 py-2 font-medium">Status</th>
                <th className="px-4 py-2 font-medium">Members</th>
                <th className="px-4 py-2 font-medium">Created</th>
                <th className="px-4 py-2 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {visibleGroups.map((group) => (
                <tr key={group.groupId} className="border-t border-border/60">
                  <td className="px-4 py-2 font-medium text-foreground">{group.name}</td>
                  <td className="px-4 py-2 font-mono text-xs">{group.code}</td>
                  <td className="px-4 py-2">
                    <StatusBadge status={group.status} />
                  </td>
                  <td className="px-4 py-2">{group.memberCount}</td>
                  <td className="px-4 py-2">{formatDate(group.createdAt)}</td>
                  <td className="px-4 py-2 text-right">
                    <GroupDetailsDialog group={group} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && (
        <PaginationControls
          page={data.page}
          totalPages={data.totalPages}
          hasNext={data.hasNext}
          hasPrevious={data.hasPrevious}
          onPageChange={setPage}
        />
      )}
    </PageContainer>
  );
}
