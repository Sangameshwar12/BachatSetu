"use client";

import { Info, LogOut, Monitor, Moon, ShieldCheck, Sun } from "lucide-react";
import { useTheme } from "next-themes";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { PageContainer } from "@/components/dashboard/page-container";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { preferredLanguages } from "@/constants/auth";
import { useAuth } from "@/contexts/auth-context";
import { useHasMounted } from "@/hooks/use-has-mounted";
import { cn } from "@/lib/utils";
import { getCachedProfile, mergeCachedProfile } from "@/lib/profile-cache";
import type { PreferredLanguage } from "@/types/auth";

const themeOptions = [
  { value: "light", label: "Light", icon: Sun },
  { value: "dark", label: "Dark", icon: Moon },
  { value: "system", label: "System", icon: Monitor },
] as const;

export function SettingsContent() {
  const { theme, setTheme } = useTheme();
  const { session, logout } = useAuth();
  const router = useRouter();
  const mounted = useHasMounted();
  const [language, setLanguage] = useState<PreferredLanguage>("ENGLISH");
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);

  // One-time hydration from this device's local cache — there's no backend endpoint to fetch
  // these from instead (see Sprint FE-3 report), so this can't be expressed as a pure external
  // snapshot the way `useHasMounted` above is.
  useEffect(() => {
    const cached = getCachedProfile();
    if (cached?.preferredLanguage) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setLanguage(cached.preferredLanguage as PreferredLanguage);
    }
    if (typeof cached?.notificationsEnabled === "boolean") {
      setNotificationsEnabled(cached.notificationsEnabled);
    }
  }, []);

  function handleLogout() {
    logout();
    router.push("/login");
  }

  return (
    <PageContainer title="Settings" description="Appearance, preferences, and account.">
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Theme</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-3 gap-2">
            {themeOptions.map((option) => {
              const Icon = option.icon;
              const isActive = mounted && theme === option.value;
              return (
                <button
                  key={option.value}
                  type="button"
                  onClick={() => setTheme(option.value)}
                  className={cn(
                    "flex flex-col items-center gap-1.5 rounded-xl border px-3 py-3 text-xs font-medium transition-colors",
                    isActive
                      ? "border-primary bg-primary/10 text-primary"
                      : "border-border/60 text-muted-foreground hover:text-foreground"
                  )}
                >
                  <Icon className="size-4" />
                  {option.label}
                </button>
              );
            })}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Language</CardTitle>
        </CardHeader>
        <CardContent>
          <Select
            value={language}
            onValueChange={(value) => {
              const next = value as PreferredLanguage;
              setLanguage(next);
              mergeCachedProfile({ preferredLanguage: next });
            }}
          >
            <SelectTrigger className="w-full max-w-xs">
              <SelectValue>
                {(value: PreferredLanguage) =>
                  preferredLanguages.find((l) => l.value === value)?.label
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {preferredLanguages.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Notifications</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-foreground">Contribution &amp; draw updates</p>
              <p className="text-xs text-muted-foreground">Payment reminders, draw results, group activity</p>
            </div>
            <Switch
              checked={notificationsEnabled}
              onCheckedChange={(checked) => {
                setNotificationsEnabled(checked);
                mergeCachedProfile({ notificationsEnabled: checked });
              }}
            />
          </div>
        </CardContent>
      </Card>

      <Alert>
        <Info />
        <AlertTitle>Language and notification preferences are saved on this device</AlertTitle>
        <AlertDescription>
          There&apos;s no backend endpoint yet to re-fetch or update these after onboarding, so
          changes here are local to this browser rather than synced to your account.
        </AlertDescription>
      </Alert>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <ShieldCheck className="size-4 text-primary" /> Privacy
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            BachatSetu stores your mobile number, group contributions, and payment records to run
            your savings group. We never share your data with other members beyond what&apos;s
            needed to run the group you&apos;re part of.
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Account</CardTitle>
        </CardHeader>
        <CardContent className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-foreground">{session?.mobileNumber}</p>
            <p className="text-xs text-muted-foreground">Signed in</p>
          </div>
          <Button variant="destructive" onClick={handleLogout}>
            <LogOut className="size-4" /> Log out
          </Button>
        </CardContent>
      </Card>
    </PageContainer>
  );
}
