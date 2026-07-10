"use client";

import { Info, Mail, MapPin, Phone, UserRound } from "lucide-react";
import { useEffect, useState } from "react";

import { PageContainer } from "@/components/dashboard/page-container";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { useAuth } from "@/contexts/auth-context";
import { getCachedProfile, type CachedProfile } from "@/lib/profile-cache";
import { preferredLanguages } from "@/constants/auth";

function ProfileRow({ icon: Icon, label, value }: { icon: typeof UserRound; label: string; value: string }) {
  return (
    <div className="flex items-center gap-3 py-2.5">
      <Icon className="size-4 shrink-0 text-muted-foreground" />
      <div className="flex flex-col">
        <span className="text-xs text-muted-foreground">{label}</span>
        <span className="text-sm font-medium text-foreground">{value}</span>
      </div>
    </div>
  );
}

export function ProfileContent() {
  const { session } = useAuth();
  const [cached, setCached] = useState<CachedProfile | null>(null);

  // One-time hydration from this device's local cache — there's no backend endpoint to fetch
  // this from instead (see Sprint FE-3 report, "no GET /users/me").
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setCached(getCachedProfile());
  }, []);

  const fullName = [cached?.givenName, cached?.familyName].filter(Boolean).join(" ");
  const languageLabel = preferredLanguages.find((l) => l.value === cached?.preferredLanguage)?.label;

  const fields = [
    Boolean(fullName),
    Boolean(cached?.email),
    Boolean(cached?.city),
    Boolean(cached?.state),
    Boolean(cached?.hasPhoto),
  ];
  const completion = Math.round((fields.filter(Boolean).length / fields.length) * 100);

  return (
    <PageContainer title="Profile" description="Your BachatSetu account details.">
      <Card>
        <CardContent className="flex flex-col items-center gap-4 pt-2 text-center">
          <Avatar size="lg" className="size-20">
            <AvatarFallback>
              <UserRound className="size-8 text-muted-foreground" />
            </AvatarFallback>
          </Avatar>
          <div>
            <h2 className="text-lg font-semibold text-foreground">{fullName || session?.mobileNumber}</h2>
            {fullName && <p className="text-sm text-muted-foreground">{session?.mobileNumber}</p>}
          </div>

          <div className="w-full max-w-xs">
            <div className="mb-1 flex items-center justify-between text-xs text-muted-foreground">
              <span>Profile completion</span>
              <span>{completion}%</span>
            </div>
            <Progress value={completion} />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="divide-y divide-border/60">
          <ProfileRow icon={Phone} label="Mobile number" value={session?.mobileNumber ?? "—"} />
          {cached?.email && <ProfileRow icon={Mail} label="Email" value={cached.email} />}
          {languageLabel && <ProfileRow icon={UserRound} label="Preferred language" value={languageLabel} />}
          {(cached?.city || cached?.state) && (
            <ProfileRow
              icon={MapPin}
              label="Location"
              value={[cached?.city, cached?.state].filter(Boolean).join(", ")}
            />
          )}
        </CardContent>
      </Card>

      <Alert>
        <Info />
        <AlertTitle>Editing isn&apos;t available yet</AlertTitle>
        <AlertDescription>
          There&apos;s no backend endpoint to fetch or update your profile after signup and
          onboarding — those details are shown here from this device&apos;s local cache, and can&apos;t
          be edited or re-synced until a <code>GET/PATCH /api/v1/users/me</code>-style endpoint
          ships. Your mobile number above is always live, read from your active session.
        </AlertDescription>
      </Alert>
    </PageContainer>
  );
}
