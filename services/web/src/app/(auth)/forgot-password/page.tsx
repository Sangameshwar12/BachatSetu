import type { Metadata } from "next";
import { Info } from "lucide-react";
import Link from "next/link";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { AuthShell } from "@/features/auth/auth-shell";

export const metadata: Metadata = { title: "Forgot password" };

export default function ForgotPasswordPage() {
  return (
    <AuthShell
      title="Forgot password"
      description="BachatSetu doesn't use passwords."
      footer={
        <>
          Remembered how you sign in?{" "}
          <Link href="/login" className="font-medium text-primary underline-offset-4 hover:underline">
            Back to log in
          </Link>
        </>
      }
    >
      <div className="flex flex-col gap-5">
        <Alert>
          <Info />
          <AlertTitle>There&apos;s no password to reset</AlertTitle>
          <AlertDescription>
            Every BachatSetu account signs in with a one-time code sent to your phone — there has
            never been a password to forget. Returning-user sign-in is still pending on the
            backend (see the Login page for details); this screen is a UI-only placeholder until
            that ships.
          </AlertDescription>
        </Alert>
        <Link href="/signup" className={cn(buttonVariants({ size: "lg" }))}>
          Create an account
        </Link>
      </div>
    </AuthShell>
  );
}
