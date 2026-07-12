"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { INDIAN_MOBILE_PATTERN } from "@/constants/auth";
import { useAuth } from "@/contexts/auth-context";
import { OtpStep } from "@/features/auth/otp-step";
import { ApiError } from "@/services/api-client";
import { loginStart, loginVerify } from "@/services/auth-service";

const mobileSchema = z.object({
  mobileNumber: z
    .string()
    .trim()
    .regex(INDIAN_MOBILE_PATTERN, "Enter a valid Indian mobile number, e.g. +919876543210"),
});

type MobileFormValues = z.infer<typeof mobileSchema>;

interface PendingLogin {
  userId: string;
  mobileNumber: string;
  otpExpiresAt: string;
}

export function LoginForm() {
  const router = useRouter();
  const { login } = useAuth();
  const [pending, setPending] = useState<PendingLogin | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<MobileFormValues>({
    resolver: zodResolver(mobileSchema),
    defaultValues: { mobileNumber: "+91" },
  });

  async function onSubmitMobile(values: MobileFormValues) {
    try {
      const result = await loginStart({ mobileNumber: values.mobileNumber });
      setPending({
        userId: result.userId,
        mobileNumber: result.mobileNumber,
        otpExpiresAt: result.otpExpiresAt,
      });
    } catch (cause) {
      if (cause instanceof ApiError && cause.code === "mobile-not-registered") {
        setError("mobileNumber", { message: "No account is registered for this mobile number." });
        return;
      }
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't start sign-in — try again.");
    }
  }

  async function handleVerify(code: string) {
    if (!pending) {
      return;
    }
    const tokens = await loginVerify({ userId: pending.userId, code });
    login(tokens);
    toast.success("Welcome back!");
    router.push("/dashboard");
  }

  if (pending) {
    return (
      <OtpStep
        userId={pending.userId}
        purpose="SIGN_IN"
        expiresAt={pending.otpExpiresAt}
        destination={pending.mobileNumber}
        onVerify={handleVerify}
        onBack={() => {
          setPending(null);
          reset();
        }}
      />
    );
  }

  return (
    <form className="flex flex-col gap-5" onSubmit={handleSubmit(onSubmitMobile)} noValidate>
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
    </form>
  );
}
