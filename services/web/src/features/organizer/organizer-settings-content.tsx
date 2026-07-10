import { Info } from "lucide-react";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { SettingsContent } from "@/features/settings/settings-content";

/**
 * Reuses the member Settings page wholesale (theme, language, notifications, privacy, logout —
 * all identical for an organizer), then adds the two organizer-specific preference sections the
 * backend doesn't yet support editing.
 */
export function OrganizerSettingsContent() {
  return (
    <div className="flex flex-col gap-6">
      <SettingsContent />

      <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 pb-6 sm:px-6 lg:px-8">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Group preferences</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              Contribution amount, schedule, and payout rules are set once when a group is
              created and can&apos;t be edited afterward — there&apos;s no backend endpoint for it
              yet. Group lifecycle (activate, suspend, close) is available from each group&apos;s
              own Settings tab.
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Invitation preferences</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              Invitations expire 7 days after generation — a fixed server-side default, not
              currently configurable per group or per organizer.
            </p>
          </CardContent>
        </Card>

        <Alert>
          <Info />
          <AlertTitle>Documented backend limitation</AlertTitle>
          <AlertDescription>
            The two cards above reflect real backend behavior (see
            <code className="mx-1 rounded bg-muted px-1 py-0.5 text-xs">
              bachatsetu.invitation.validity
            </code>
            and group creation rules) rather than settings you can currently change.
          </AlertDescription>
        </Alert>
      </div>
    </div>
  );
}
