import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { AuthShell } from "@/features/auth/auth-shell";
import { SignupForm } from "@/features/auth/signup-form";
import { isSafeRedirectPath } from "@/lib/utils";

export const metadata: Metadata = { title: "Create your account" };

export default async function SignupPage({
  searchParams,
}: {
  searchParams: Promise<{ redirect?: string }>;
}) {
  const { redirect } = await searchParams;
  const loginHref = isSafeRedirectPath(redirect) ? `/login?redirect=${encodeURIComponent(redirect)}` : "/login";

  return (
    <AuthShell
      title="Create your account"
      description="Verify your number to start your first savings group."
      footer={
        <>
          Already have an account?{" "}
          <Link href={loginHref} className="font-medium text-primary underline-offset-4 hover:underline">
            Log in
          </Link>
        </>
      }
    >
      <Suspense>
        <SignupForm />
      </Suspense>
    </AuthShell>
  );
}
