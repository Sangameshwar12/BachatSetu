"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { INDIAN_MOBILE_PATTERN, preferredLanguages } from "@/constants/auth";
import { useAuth } from "@/contexts/auth-context";
import { OtpStep } from "@/features/auth/otp-step";
import { mergeCachedProfile } from "@/lib/profile-cache";
import { ApiError } from "@/services/api-client";
import { signupStart, signupVerify } from "@/services/auth-service";
import type { PreferredLanguage } from "@/types/auth";

const detailsSchema = z.object({
  givenName: z.string().trim().min(1, "Enter your first name").max(100),
  familyName: z.string().trim().max(100).optional().or(z.literal("")),
  mobileNumber: z
    .string()
    .trim()
    .regex(INDIAN_MOBILE_PATTERN, "Enter a valid Indian mobile number, e.g. +919876543210"),
  email: z.string().trim().email("Enter a valid email").max(254).optional().or(z.literal("")),
  preferredLanguage: z.enum(["ENGLISH", "HINDI", "MARATHI"]),
  acceptedTerms: z
    .boolean()
    .refine((value) => value === true, "You must accept the terms and conditions to continue"),
});

type DetailsFormValues = z.infer<typeof detailsSchema>;

interface PendingSignup {
  userId: string;
  mobileNumber: string;
  otpExpiresAt: string;
}

export function SignupForm() {
  const router = useRouter();
  const { login } = useAuth();
  const [pending, setPending] = useState<PendingSignup | null>(null);

  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DetailsFormValues>({
    resolver: zodResolver(detailsSchema),
    defaultValues: {
      givenName: "",
      familyName: "",
      mobileNumber: "+91",
      email: "",
      preferredLanguage: "ENGLISH",
      acceptedTerms: false,
    },
  });

  async function onSubmitDetails(values: DetailsFormValues) {
    try {
      const result = await signupStart({
        givenName: values.givenName,
        familyName: values.familyName || undefined,
        mobileNumber: values.mobileNumber,
        email: values.email || undefined,
        preferredLanguage: values.preferredLanguage,
        acceptedTerms: values.acceptedTerms,
      });
      setPending({
        userId: result.userId,
        mobileNumber: result.mobileNumber,
        otpExpiresAt: result.otpExpiresAt,
      });
      // No GET /users/me exists to re-fetch these later — cache what the user just told us.
      mergeCachedProfile({
        givenName: values.givenName,
        familyName: values.familyName || undefined,
        email: values.email || undefined,
        preferredLanguage: values.preferredLanguage,
      });
    } catch (cause) {
      if (cause instanceof ApiError && cause.code === "mobile-already-registered") {
        setError("mobileNumber", { message: "This mobile number is already registered." });
        return;
      }
      if (cause instanceof ApiError && cause.code === "email-already-registered") {
        setError("email", { message: "This email is already registered." });
        return;
      }
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't start signup — try again.");
    }
  }

  async function handleVerify(code: string) {
    if (!pending) {
      return;
    }
    const tokens = await signupVerify({ userId: pending.userId, code });
    login(tokens);
    toast.success("Account verified — let's finish setting up your profile.");
    router.push("/onboarding");
  }

  if (pending) {
    return (
      <OtpStep
        userId={pending.userId}
        purpose="REGISTRATION"
        expiresAt={pending.otpExpiresAt}
        destination={pending.mobileNumber}
        onVerify={handleVerify}
        onBack={() => setPending(null)}
      />
    );
  }

  return (
    <form className="flex flex-col gap-5" onSubmit={handleSubmit(onSubmitDetails)} noValidate>
      <div className="grid grid-cols-2 gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="givenName">First name</Label>
          <Input
            id="givenName"
            autoComplete="given-name"
            aria-invalid={errors.givenName ? true : undefined}
            disabled={isSubmitting}
            {...register("givenName")}
          />
          {errors.givenName && <p className="text-sm text-destructive">{errors.givenName.message}</p>}
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="familyName">Last name</Label>
          <Input
            id="familyName"
            autoComplete="family-name"
            disabled={isSubmitting}
            {...register("familyName")}
          />
        </div>
      </div>

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

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="email">Email (optional)</Label>
        <Input
          id="email"
          type="email"
          autoComplete="email"
          aria-invalid={errors.email ? true : undefined}
          disabled={isSubmitting}
          {...register("email")}
        />
        {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="preferredLanguage">Preferred language</Label>
        <Controller
          control={control}
          name="preferredLanguage"
          render={({ field }) => (
            <Select
              value={field.value}
              onValueChange={(value) => field.onChange(value as PreferredLanguage)}
            >
              <SelectTrigger id="preferredLanguage" className="w-full">
                <SelectValue>
                  {(value: PreferredLanguage) =>
                    preferredLanguages.find((language) => language.value === value)?.label
                  }
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                {preferredLanguages.map((language) => (
                  <SelectItem key={language.value} value={language.value}>
                    {language.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        />
      </div>

      <Controller
        control={control}
        name="acceptedTerms"
        render={({ field }) => (
          <div className="flex flex-col gap-1.5">
            <div className="group/field flex items-start gap-2.5">
              <Checkbox
                id="acceptedTerms"
                checked={field.value}
                onCheckedChange={(checked) => field.onChange(checked === true)}
                disabled={isSubmitting}
              />
              <Label htmlFor="acceptedTerms" className="text-sm leading-snug font-normal text-muted-foreground">
                I accept the Terms of Service and Privacy Policy
              </Label>
            </div>
            {errors.acceptedTerms && (
              <p className="text-sm text-destructive">{errors.acceptedTerms.message}</p>
            )}
          </div>
        )}
      />

      <Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>
        {isSubmitting && <Loader2 className="size-4 animate-spin" />}
        Create account
      </Button>
    </form>
  );
}
