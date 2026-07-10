"use client";

import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useSendBroadcast } from "@/hooks/use-broadcast";
import { ApiError } from "@/services/api-client";
import type { BroadcastScope } from "@/types/platform-operations";

const SCOPES: { value: BroadcastScope; label: string }[] = [
  { value: "ALL_USERS", label: "All users" },
  { value: "TENANT", label: "One tenant" },
  { value: "ORGANIZERS", label: "Organizers only" },
  { value: "MEMBERS", label: "Members only" },
];

export function AdminBroadcastForm() {
  const [scope, setScope] = useState<BroadcastScope>("ALL_USERS");
  const [tenantId, setTenantId] = useState("");
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const sendBroadcast = useSendBroadcast();

  return (
    <Card>
      <CardHeader>
        <CardTitle>Broadcast notification</CardTitle>
      </CardHeader>
      <CardContent>
        <form
          className="flex max-w-xl flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault();
            sendBroadcast.mutate(
              { scope, tenantId: scope === "TENANT" ? tenantId : undefined, title, message },
              {
                onError: (cause) =>
                  toast.error(cause instanceof ApiError ? cause.message : "Couldn't send the broadcast."),
              }
            );
          }}
        >
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="broadcast-scope">Scope</Label>
            <Select value={scope} onValueChange={(value) => setScope(value as BroadcastScope)}>
              <SelectTrigger id="broadcast-scope" className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {SCOPES.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {scope === "TENANT" && (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="broadcast-tenant">Tenant ID</Label>
              <Input id="broadcast-tenant" value={tenantId} onChange={(event) => setTenantId(event.target.value)} required />
            </div>
          )}

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="broadcast-title">Title</Label>
            <Input
              id="broadcast-title"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              maxLength={200}
              required
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="broadcast-message">Message</Label>
            <Textarea
              id="broadcast-message"
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              maxLength={2000}
              required
            />
          </div>

          <Button type="submit" disabled={sendBroadcast.isPending} className="w-fit">
            Send broadcast
          </Button>

          {sendBroadcast.data && (
            <p className="text-xs text-success">
              Sent to {sendBroadcast.data.sentCount} of {sendBroadcast.data.recipientCount} recipients
              {sendBroadcast.data.failedCount > 0 ? ` (${sendBroadcast.data.failedCount} failed)` : ""}.
            </p>
          )}
        </form>
      </CardContent>
    </Card>
  );
}
