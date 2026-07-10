"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, Info, Loader2 } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { INDIAN_MOBILE_PATTERN } from "@/constants/auth";
import { cn } from "@/lib/utils";

const loginSchema = z.object({
  mobileNumber: z
    .string()
    .trim()
    .regex(INDIAN_MOBILE_PATTERN, "Enter a valid Indian mobile number, e.g. +919876543210"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export function LoginForm() {
  const [showGapNotice, setShowGapNotice] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { mobileNumber: "+91" },
  });

  async function onSubmit() {
    // See Sprint FE-2 report, "Pending Backend APIs": there is no backend endpoint that issues
    // tokens for a returning user from just a mobile number (no lookup-by-mobile, no SIGN_IN
    // token issuance). Rather than inventing one, this surfaces the gap honestly.
    await new Promise((resolve) => setTimeout(resolve, 400));
    setShowGapNotice(true);
  }

  if (showGapNotice) {
    return (
      <div className="flex flex-col gap-5">
        <Alert>
          <Info />
          <AlertTitle>Sign-in for returning users isn&apos;t live yet</AlertTitle>
          <AlertDescription>
            The backend doesn&apos;t yet expose a way to sign in with just a phone number — that
            needs a mobile-lookup and token-issuing endpoint that hasn&apos;t shipped. New
            accounts can already sign up and start using BachatSetu today.
          </AlertDescription>
        </Alert>
        <Link href="/signup" className={cn(buttonVariants({ size: "lg" }))}>
          Create an account instead <ArrowRight className="size-4" />
        </Link>
        <button
          type="button"
          onClick={() => {
            setShowGapNotice(false);
            reset();
          }}
          className="text-sm text-muted-foreground underline-offset-4 hover:underline"
        >
          Try a different number
        </button>
      </div>
    );
  }

  return (
    <form className="flex flex-col gap-5" onSubmit={handleSubmit(onSubmit)} noValidate>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="mobileNumber">Mobile number</Label>
        <Input
          id="mobileNumber"
          type="tel"
          autoComplete="tel"
          placeholder="+919876543210"
          aria-invalid={errors.mobileNumber ? true : undefined}
          disabled={isSubmitting}
          {...register("mobileNumber")}
        />
        {errors.mobileNumber && (
          <p className="text-sm text-destructive">{errors.mobileNumber.message}</p>
        )}
      </div>

      <Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>
        {isSubmitting && <Loader2 className="size-4 animate-spin" />}
        Continue
      </Button>

      <Link
        href="/forgot-password"
        className="text-center text-sm text-muted-foreground underline-offset-4 hover:underline"
      >
        Forgot password?
      </Link>
    </form>
  );
}
