"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, UserRound } from "lucide-react";
import { useRouter } from "next/navigation";
import { useRef, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { mergeCachedProfile } from "@/lib/profile-cache";
import { ApiError } from "@/services/api-client";
import { completeOnboarding } from "@/services/onboarding-service";
import { uploadFile } from "@/services/storage-service";

const onboardingSchema = z.object({
  city: z.string().trim().max(100).optional().or(z.literal("")),
  state: z.string().trim().max(100).optional().or(z.literal("")),
  notificationsEnabled: z.boolean(),
});

type OnboardingFormValues = z.infer<typeof onboardingSchema>;

const MAX_PHOTO_BYTES = 5 * 1024 * 1024;

export function OnboardingForm() {
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [photoFileId, setPhotoFileId] = useState<string | null>(null);
  const [photoPreviewUrl, setPhotoPreviewUrl] = useState<string | null>(null);
  const [isUploadingPhoto, setIsUploadingPhoto] = useState(false);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<OnboardingFormValues>({
    resolver: zodResolver(onboardingSchema),
    defaultValues: { city: "", state: "", notificationsEnabled: true },
  });

  async function handlePhotoSelected(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    if (!file.type.startsWith("image/")) {
      toast.error("Please choose an image file.");
      event.target.value = "";
      return;
    }
    if (file.size > MAX_PHOTO_BYTES) {
      toast.error("That photo is too large — please choose one under 5 MB.");
      event.target.value = "";
      return;
    }
    setIsUploadingPhoto(true);
    try {
      const result = await uploadFile(file);
      setPhotoFileId(result.fileId);
      setPhotoPreviewUrl(URL.createObjectURL(file));
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't upload that photo — try again.");
    } finally {
      setIsUploadingPhoto(false);
    }
  }

  async function onSubmit(values: OnboardingFormValues) {
    try {
      await completeOnboarding({
        city: values.city || undefined,
        state: values.state || undefined,
        photoFileId: photoFileId ?? undefined,
        notificationsEnabled: values.notificationsEnabled,
      });
      // No GET /users/me exists to re-fetch these later — cache what the user just told us.
      mergeCachedProfile({
        city: values.city || undefined,
        state: values.state || undefined,
        notificationsEnabled: values.notificationsEnabled,
        hasPhoto: Boolean(photoFileId),
      });
      toast.success("You're all set up.");
      router.push("/dashboard");
    } catch (cause) {
      if (cause instanceof ApiError && cause.code === "already-onboarded") {
        router.push("/dashboard");
        return;
      }
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't save your profile — try again.");
    }
  }

  return (
    <form className="flex flex-col gap-6" onSubmit={handleSubmit(onSubmit)} noValidate>
      <div className="flex flex-col items-center gap-3">
        <Avatar size="lg" className="size-20">
          <AvatarImage src={photoPreviewUrl ?? undefined} alt="" />
          <AvatarFallback>
            <UserRound className="size-8 text-muted-foreground" />
          </AvatarFallback>
        </Avatar>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={handlePhotoSelected}
        />
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={isUploadingPhoto}
          onClick={() => fileInputRef.current?.click()}
        >
          {isUploadingPhoto && <Loader2 className="size-4 animate-spin" />}
          {photoFileId ? "Change photo" : "Add a photo"}
        </Button>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="city">City</Label>
          <Input id="city" autoComplete="address-level2" disabled={isSubmitting} {...register("city")} />
          {errors.city && <p className="text-sm text-destructive">{errors.city.message}</p>}
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="state">State</Label>
          <Input id="state" autoComplete="address-level1" disabled={isSubmitting} {...register("state")} />
          {errors.state && <p className="text-sm text-destructive">{errors.state.message}</p>}
        </div>
      </div>

      <Controller
        control={control}
        name="notificationsEnabled"
        render={({ field }) => (
          <div className="flex items-center justify-between rounded-lg border border-border/60 px-3.5 py-3">
            <div className="flex flex-col gap-0.5">
              <Label htmlFor="notificationsEnabled">Notifications</Label>
              <p className="text-xs text-muted-foreground">
                Contribution reminders, draw results, and group updates
              </p>
            </div>
            <Switch
              id="notificationsEnabled"
              checked={field.value}
              onCheckedChange={field.onChange}
              disabled={isSubmitting}
            />
          </div>
        )}
      />

      <Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>
        {isSubmitting && <Loader2 className="size-4 animate-spin" />}
        Finish setting up
      </Button>
    </form>
  );
}
