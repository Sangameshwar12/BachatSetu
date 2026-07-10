"use client";

import { Search, Users } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { PageContainer } from "@/components/dashboard/page-container";
import { PaginationControls } from "@/components/dashboard/pagination-controls";
import { StatusBadge } from "@/components/dashboard/status-badge";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { useAdminUsers, useDisableUser, useEnableUser } from "@/hooks/use-admin-users";
import { ApiError } from "@/services/api-client";
import type { PlatformUserResponse } from "@/types/admin";
import { formatDate } from "@/utils/format";

const PAGE_SIZE = 20;
const STATUS_OPTIONS = ["ACTIVE", "PENDING_VERIFICATION", "LOCKED", "SUSPENDED", "DISABLED"];

function UserRowAction({ user }: { user: PlatformUserResponse }) {
  const enableUser = useEnableUser();
  const disableUser = useDisableUser();
  const isDisabled = user.status === "DISABLED";
  const mutation = isDisabled ? enableUser : disableUser;
  const action = isDisabled ? "enable" : "disable";

  function handleConfirm() {
    mutation.mutate(user.userId, {
      onSuccess: () => toast.success(`User ${action}d.`),
      onError: (cause) =>
        toast.error(cause instanceof ApiError ? cause.message : `Couldn't ${action} this user.`),
    });
  }

  return (
    <AlertDialog>
      <AlertDialogTrigger
        render={
          <Button variant={isDisabled ? "outline" : "destructive"} size="sm" />
        }
      >
        {isDisabled ? "Enable" : "Disable"}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{isDisabled ? "Enable this user?" : "Disable this user?"}</AlertDialogTitle>
          <AlertDialogDescription>
            {isDisabled
              ? `${user.email ?? user.phoneNumber ?? user.userId} will be able to sign in again.`
              : `${user.email ?? user.phoneNumber ?? user.userId} will be immediately signed out and unable to sign in.`}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancel</AlertDialogCancel>
          <AlertDialogAction
            variant={isDisabled ? "default" : "destructive"}
            onClick={handleConfirm}
            disabled={mutation.isPending}
          >
            {isDisabled ? "Enable" : "Disable"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

export function AdminUsersContent() {
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);

  const { data, isPending, isError, error, refetch } = useAdminUsers({
    email: email || undefined,
    phone: phone || undefined,
    status,
    page,
    size: PAGE_SIZE,
  });

  return (
    <PageContainer title="User Management" description="Search and manage users across every tenant.">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1 sm:max-w-xs">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Filter by email"
            className="pl-8"
            value={email}
            onChange={(event) => {
              setPage(0);
              setEmail(event.target.value);
            }}
          />
        </div>
        <Input
          placeholder="Filter by phone"
          className="sm:max-w-xs"
          value={phone}
          onChange={(event) => {
            setPage(0);
            setPhone(event.target.value);
          }}
        />
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

      {isPending ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 6 }, (_, index) => (
            <Skeleton key={index} className="h-12 rounded-lg" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState icon={Users} title="No users match these filters" description="Try clearing the search or status filter." />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-border/60">
          <table className="w-full text-left text-sm">
            <thead className="bg-muted/40 text-xs text-muted-foreground">
              <tr>
                <th className="px-4 py-2 font-medium">Email</th>
                <th className="px-4 py-2 font-medium">Phone</th>
                <th className="px-4 py-2 font-medium">Status</th>
                <th className="px-4 py-2 font-medium">Created</th>
                <th className="px-4 py-2 font-medium">Tenant</th>
                <th className="px-4 py-2 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((user) => (
                <tr key={user.userId} className="border-t border-border/60">
                  <td className="px-4 py-2">{user.email ?? "—"}</td>
                  <td className="px-4 py-2">{user.phoneNumber ?? "—"}</td>
                  <td className="px-4 py-2">
                    <StatusBadge status={user.status} />
                  </td>
                  <td className="px-4 py-2">{formatDate(user.createdAt)}</td>
                  <td className="px-4 py-2 font-mono text-xs text-muted-foreground">{user.tenantId}</td>
                  <td className="px-4 py-2 text-right">
                    <UserRowAction user={user} />
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
