"use client";

import { ArrowRight, Loader2, QrCode, Ticket, Users } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";

import { PageContainer } from "@/components/dashboard/page-container";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApiError } from "@/services/api-client";
import { useInvitationPreview, useJoinGroup } from "@/hooks/use-invitation";
import type { JoinChannel } from "@/types/invitation";
import { formatPaiseAsRupees } from "@/utils/format";

/** Extracts an invitation token from a pasted link (`.../join/<token>`) or a bare token. */
function extractToken(input: string): string {
  const trimmed = input.trim();
  const marker = "/join/";
  const markerIndex = trimmed.indexOf(marker);
  if (markerIndex >= 0) {
    return trimmed.slice(markerIndex + marker.length).split(/[?#]/)[0];
  }
  return trimmed;
}

export function JoinGroupContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [linkInput, setLinkInput] = useState(searchParams.get("token") ?? "");
  const [codeInput, setCodeInput] = useState("");

  const token = extractToken(linkInput);
  const preview = useInvitationPreview(token);
  const joinGroup = useJoinGroup();

  async function handleJoin(payload: { code?: string; token?: string; channel: JoinChannel }) {
    try {
      const result = await joinGroup.mutateAsync(payload);
      toast.success("You've joined the group.");
      router.push(`/dashboard/groups/${result.groupId}`);
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't join that group — try again.");
    }
  }

  return (
    <PageContainer title="Join a group" description="Use an invitation code, link, or QR code.">
      <div className="mx-auto w-full max-w-lg">
        <Tabs defaultValue="link">
          <TabsList className="w-full">
            <TabsTrigger value="link">
              <QrCode className="size-4" /> Link or QR
            </TabsTrigger>
            <TabsTrigger value="code">
              <Ticket className="size-4" /> Invite code
            </TabsTrigger>
          </TabsList>

          <TabsContent value="link" className="pt-4">
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="joinLink">Invitation link or token</Label>
                <Input
                  id="joinLink"
                  placeholder="https://bachatsetu.example.com/join/AbCdEf123..."
                  value={linkInput}
                  onChange={(event) => setLinkInput(event.target.value)}
                />
                <p className="text-xs text-muted-foreground">
                  Scanned a QR code? Open the link it takes you to, then paste it here.
                </p>
              </div>

              {token && preview.isPending && (
                <p className="text-sm text-muted-foreground">Looking up this invitation…</p>
              )}

              {token && preview.isError && (
                <p className="text-sm text-destructive">
                  {preview.error instanceof ApiError
                    ? preview.error.message
                    : "This invitation link doesn't look right."}
                </p>
              )}

              {preview.data && (
                <Card>
                  <CardHeader>
                    <CardTitle className="text-base">{preview.data.groupName}</CardTitle>
                  </CardHeader>
                  <CardContent className="flex flex-col gap-3">
                    <div className="grid grid-cols-2 gap-3 text-sm">
                      <div>
                        <p className="text-xs text-muted-foreground">Organizer</p>
                        <p className="font-medium text-foreground">{preview.data.organizerName}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">Monthly amount</p>
                        <p className="font-medium text-foreground">
                          {formatPaiseAsRupees(preview.data.contributionAmountPaise)}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">Frequency</p>
                        <p className="font-medium text-foreground">{preview.data.frequency}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">Members</p>
                        <p className="inline-flex items-center gap-1 font-medium text-foreground">
                          <Users className="size-3.5" />
                          {preview.data.memberCount}/{preview.data.maximumMembers}
                        </p>
                      </div>
                    </div>
                    <Button
                      className="w-full"
                      disabled={joinGroup.isPending}
                      onClick={() => handleJoin({ token, channel: "LINK" })}
                    >
                      {joinGroup.isPending && <Loader2 className="size-4 animate-spin" />}
                      Join this group <ArrowRight className="size-4" />
                    </Button>
                  </CardContent>
                </Card>
              )}
            </div>
          </TabsContent>

          <TabsContent value="code" className="pt-4">
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="inviteCode">Invitation code</Label>
                <Input
                  id="inviteCode"
                  placeholder="AB3D9F2K"
                  value={codeInput}
                  onChange={(event) => setCodeInput(event.target.value.toUpperCase())}
                />
                <p className="text-xs text-muted-foreground">
                  A typed code joins directly — there&apos;s no preview for codes yet, only for
                  links and QR codes.
                </p>
              </div>
              <Button
                className="w-full"
                disabled={!codeInput.trim() || joinGroup.isPending}
                onClick={() => handleJoin({ code: codeInput.trim(), channel: "CODE" })}
              >
                {joinGroup.isPending && <Loader2 className="size-4 animate-spin" />}
                Join with code
              </Button>
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </PageContainer>
  );
}
