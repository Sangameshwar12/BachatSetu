"use client";

import { Loader2, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp";
import { OTP_CODE_LENGTH } from "@/constants/auth";
import { useCountdown } from "@/hooks/use-countdown";
import { ApiError } from "@/services/api-client";
import { resendOtp } from "@/services/auth-service";
import type { OtpPurpose } from "@/types/auth";

interface OtpStepProps {
  userId: string;
  purpose: OtpPurpose;
  /** ISO timestamp the active challenge expires at — seeds the resend countdown. */
  expiresAt: string;
  /** Destination the code was sent to, shown for reassurance (e.g. "+91 98765 43210"). */
  destination: string;
  onVerify: (code: string) => Promise<void>;
  onBack?: () => void;
}

export function OtpStep({ userId, purpose, expiresAt, destination, onVerify, onBack }: OtpStepProps) {
  const [code, setCode] = useState("");
  const [currentExpiresAt, setCurrentExpiresAt] = useState(expiresAt);
  const [isVerifying, setIsVerifying] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const remainingMs = useCountdown(new Date(currentExpiresAt).getTime());
  const remainingSeconds = Math.ceil(remainingMs / 1000);
  const canResend = remainingMs <= 0 && !isResending;

  async function handleVerify() {
    if (code.length !== OTP_CODE_LENGTH) {
      return;
    }
    setError(null);
    setIsVerifying(true);
    try {
      await onVerify(code);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Something went wrong. Please try again.");
      setCode("");
    } finally {
      setIsVerifying(false);
    }
  }

  async function handleResend() {
    setIsResending(true);
    setError(null);
    try {
      const result = await resendOtp({ userId, purpose });
      setCurrentExpiresAt(result.expiresAt);
      setCode("");
      toast.success("A new code is on its way.");
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't resend the code — try again shortly.");
    } finally {
      setIsResending(false);
    }
  }

  return (
    <div className="flex flex-col items-center gap-6 text-center">
      <div className="flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
        <ShieldCheck className="size-6" />
      </div>

      <div className="flex flex-col gap-1.5">
        <h2 className="text-lg font-semibold text-foreground">Enter the code we sent you</h2>
        <p className="text-sm text-muted-foreground">
          6 digits sent to <span className="font-medium text-foreground">{destination}</span>
        </p>
      </div>

      <InputOTP
        maxLength={OTP_CODE_LENGTH}
        value={code}
        onChange={(value) => {
          setCode(value);
          setError(null);
        }}
        disabled={isVerifying}
        aria-invalid={error ? true : undefined}
      >
        <InputOTPGroup>
          {Array.from({ length: OTP_CODE_LENGTH }, (_, index) => (
            <InputOTPSlot key={index} index={index} />
          ))}
        </InputOTPGroup>
      </InputOTP>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Button
        className="w-full max-w-xs"
        size="lg"
        disabled={code.length !== OTP_CODE_LENGTH || isVerifying}
        onClick={handleVerify}
      >
        {isVerifying && <Loader2 className="size-4 animate-spin" />}
        Verify and continue
      </Button>

      <div className="flex flex-col items-center gap-2 text-sm text-muted-foreground">
        {canResend ? (
          <button
            type="button"
            className="font-medium text-primary underline-offset-4 hover:underline disabled:opacity-50"
            onClick={handleResend}
            disabled={isResending}
          >
            {isResending ? "Sending a new code…" : "Resend code"}
          </button>
        ) : (
          <p>
            Resend available in{" "}
            <span className="font-medium tabular-nums text-foreground">{remainingSeconds}s</span>
          </p>
        )}
        {onBack && (
          <button
            type="button"
            className="text-muted-foreground underline-offset-4 hover:underline"
            onClick={onBack}
          >
            Use a different number
          </button>
        )}
      </div>
    </div>
  );
}
