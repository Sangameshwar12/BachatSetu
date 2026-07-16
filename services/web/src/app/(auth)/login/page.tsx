import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { AuthShell } from "@/features/auth/auth-shell";
import { LoginForm } from "@/features/auth/login-form";
import { isSafeRedirectPath } from "@/lib/utils";

export const metadata: Metadata = { title: "Log in" };

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ redirect?: string }>;
}) {
  const { redirect } = await searchParams;
  const signupHref = isSafeRedirectPath(redirect) ? `/signup?redirect=${encodeURIComponent(redirect)}` : "/signup";

  return (
    <AuthShell
      title="Welcome back"
      description="Enter your mobile number to continue."
      footer={
        <>
          New to BachatSetu?{" "}
          <Link href={signupHref} className="font-medium text-primary underline-offset-4 hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      <Suspense>
        <LoginForm />
      </Suspense>
    </AuthShell>
  );
}
