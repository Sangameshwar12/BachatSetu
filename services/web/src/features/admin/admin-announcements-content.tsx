"use client";

import { Megaphone } from "lucide-react";
import { useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useAnnouncements, usePublishAnnouncement } from "@/hooks/use-announcements";
import type { AnnouncementSeverity } from "@/types/platform-operations";
import { formatDateTime } from "@/utils/format";

const SEVERITIES: AnnouncementSeverity[] = ["INFO", "WARNING", "CRITICAL"];

function severityBadgeClass(severity: AnnouncementSeverity): string {
  if (severity === "CRITICAL") return "bg-destructive/10 text-destructive";
  if (severity === "WARNING") return "bg-warning/10 text-warning";
  return "bg-info/10 text-info";
}

export function AdminAnnouncementsContent() {
  const { data, isPending, isError, error, refetch } = useAnnouncements(0, 20);
  const publishAnnouncement = usePublishAnnouncement();

  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const [severity, setSeverity] = useState<AnnouncementSeverity>("INFO");
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <CardTitle>Publish announcement</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            className="flex max-w-xl flex-col gap-4"
            onSubmit={(event) => {
              event.preventDefault();
              publishAnnouncement.mutate(
                {
                  title,
                  message,
                  severity,
                  startAt: new Date(startAt).toISOString(),
                  endAt: new Date(endAt).toISOString(),
                },
                {
                  onSuccess: () => {
                    setTitle("");
                    setMessage("");
                    setStartAt("");
                    setEndAt("");
                  },
                }
              );
            }}
          >
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="announcement-title">Title</Label>
              <Input id="announcement-title" value={title} onChange={(event) => setTitle(event.target.value)} maxLength={200} required />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="announcement-message">Message</Label>
              <Textarea
                id="announcement-message"
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                maxLength={4000}
                required
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="announcement-start">Starts at</Label>
                <Input id="announcement-start" type="datetime-local" value={startAt} onChange={(event) => setStartAt(event.target.value)} required />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="announcement-end">Ends at</Label>
                <Input id="announcement-end" type="datetime-local" value={endAt} onChange={(event) => setEndAt(event.target.value)} required />
              </div>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="announcement-severity">Severity</Label>
              <Select value={severity} onValueChange={(value) => setSeverity(value as AnnouncementSeverity)}>
                <SelectTrigger id="announcement-severity" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SEVERITIES.map((option) => (
                    <SelectItem key={option} value={option}>
                      {option}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <Button type="submit" disabled={publishAnnouncement.isPending} className="w-fit">
              Publish
            </Button>
            {publishAnnouncement.isSuccess && <p className="text-xs text-success">Announcement published.</p>}
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>All announcements</CardTitle>
        </CardHeader>
        <CardContent>
          {isPending ? (
            <div className="flex flex-col gap-2">
              {Array.from({ length: 3 }, (_, index) => (
                <Skeleton key={index} className="h-16 rounded-lg" />
              ))}
            </div>
          ) : isError ? (
            <ErrorState error={error} onRetry={() => refetch()} />
          ) : data.content.length === 0 ? (
            <EmptyState icon={Megaphone} title="No announcements yet" description="Published announcements will appear here." />
          ) : (
            <div className="flex flex-col gap-2">
              {data.content.map((announcement) => (
                <div key={announcement.announcementId} className="rounded-lg border border-border/60 px-3 py-2.5">
                  <div className="flex items-center justify-between gap-2">
                    <p className="font-medium text-foreground">{announcement.title}</p>
                    <div className="flex items-center gap-2">
                      {announcement.active && <Badge className="bg-success/10 text-success">Active</Badge>}
                      <Badge variant="outline" className={severityBadgeClass(announcement.severity)}>
                        {announcement.severity}
                      </Badge>
                    </div>
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground">{announcement.message}</p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {formatDateTime(announcement.startAt)} — {formatDateTime(announcement.endAt)}
                  </p>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
