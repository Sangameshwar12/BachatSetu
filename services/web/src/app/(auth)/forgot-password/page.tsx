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
            never been a password to forget. Head back to the login page and enter your mobile
            number to receive a fresh code.
          </AlertDescription>
        </Alert>
        <Link href="/login" className={cn(buttonVariants({ size: "lg" }))}>
          Back to log in
        </Link>
      </div>
    </AuthShell>
  );
}
