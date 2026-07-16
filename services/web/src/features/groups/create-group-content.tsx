"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { PageContainer } from "@/components/dashboard/page-container";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { contributionFrequencies, groupTypes, payoutMethods } from "@/constants/group";
import { useCreateGroup } from "@/hooks/use-group";
import { ApiError } from "@/services/api-client";
import type { ContributionFrequency, GroupType, PayoutMethod } from "@/types/group";

/** A digits-only (optionally decimal) numeric field kept as a string to avoid the zod
 * coerce-number/react-hook-form default-value generic mismatch — converted to a number only
 * once, in the submit handler, right before building the API payload. */
function positiveNumberField(message: string) {
  return z
    .string()
    .trim()
    .min(1, message)
    .refine((value) => Number.isFinite(Number(value)) && Number(value) > 0, message);
}

const createGroupSchema = z
  .object({
    name: z.string().trim().min(3, "Name must be at least 3 characters").max(100),
    description: z.string().trim().max(500).optional().or(z.literal("")),
    type: z.enum(["BHISHI", "SELF_HELP_GROUP", "SOCIETY_COLLECTION", "COMMUNITY_FUND"]),
    contributionAmount: positiveNumberField("Enter a contribution amount greater than zero"),
    frequency: z.enum(["WEEKLY", "MONTHLY", "QUARTERLY"]),
    startDate: z.string().min(1, "Select a start date"),
    cycleCount: z
      .string()
      .trim()
      .refine((value) => {
        const parsed = Number(value);
        return Number.isInteger(parsed) && parsed >= 1 && parsed <= 120;
      }, "Must run for between 1 and 120 cycles"),
    minimumMembers: z
      .string()
      .trim()
      .refine((value) => {
        const parsed = Number(value);
        return Number.isInteger(parsed) && parsed >= 2;
      }, "At least 2 members are required"),
    maximumMembers: z
      .string()
      .trim()
      .refine((value) => {
        const parsed = Number(value);
        return Number.isInteger(parsed) && parsed >= 2 && parsed <= 500;
      }, "Must be between 2 and 500 members"),
    payoutMethod: z.enum(["FIXED_ROTATION", "RANDOM_DRAW", "AUCTION"]),
    partialPaymentsAllowed: z.boolean(),
  })
  .refine((values) => Number(values.maximumMembers) >= Number(values.minimumMembers), {
    message: "Maximum members must be at least the minimum members",
    path: ["maximumMembers"],
  });

type CreateGroupFormValues = z.infer<typeof createGroupSchema>;

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}

export function CreateGroupContent() {
  const router = useRouter();
  const createGroup = useCreateGroup();

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<CreateGroupFormValues>({
    resolver: zodResolver(createGroupSchema),
    defaultValues: {
      name: "",
      description: "",
      type: "BHISHI",
      contributionAmount: "",
      frequency: "MONTHLY",
      startDate: todayIsoDate(),
      cycleCount: "12",
      minimumMembers: "2",
      maximumMembers: "10",
      payoutMethod: "RANDOM_DRAW",
      partialPaymentsAllowed: false,
    },
  });

  async function onSubmit(values: CreateGroupFormValues) {
    try {
      await createGroup.mutateAsync({
        name: values.name,
        description: values.description || undefined,
        type: values.type,
        rule: {
          contributionSchedule: {
            contributionAmountPaise: Math.round(Number(values.contributionAmount) * 100),
            frequency: values.frequency,
            startDate: values.startDate,
            cycleCount: Number(values.cycleCount),
          },
          memberCapacity: {
            minimum: Number(values.minimumMembers),
            maximum: Number(values.maximumMembers),
          },
          payoutMethod: values.payoutMethod,
          partialPaymentsAllowed: values.partialPaymentsAllowed,
        },
      });
      toast.success("Group created successfully.");
      router.push("/dashboard/groups");
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't create the group — try again.");
    }
  }

  return (
    <PageContainer title="Create a group" description="Set up a new Bhishi savings group you'll organize.">
      <div className="mx-auto w-full max-w-2xl">
        <form className="flex flex-col gap-6" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Card>
            <CardHeader>
              <CardTitle>Group details</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="name">Group name</Label>
                <Input
                  id="name"
                  placeholder="Sunrise Bhishi Circle"
                  aria-invalid={errors.name ? true : undefined}
                  disabled={isSubmitting}
                  {...register("name")}
                />
                {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="description">Description (optional)</Label>
                <Textarea
                  id="description"
                  placeholder="Monthly savings for our society"
                  aria-invalid={errors.description ? true : undefined}
                  disabled={isSubmitting}
                  {...register("description")}
                />
                {errors.description && (
                  <p className="text-sm text-destructive">{errors.description.message}</p>
                )}
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="type">Group type</Label>
                <Controller
                  control={control}
                  name="type"
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={(value) => field.onChange(value as GroupType)}>
                      <SelectTrigger id="type" className="w-full" disabled={isSubmitting}>
                        <SelectValue>
                          {(value: GroupType) => groupTypes.find((option) => option.value === value)?.label}
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        {groupTypes.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Contribution schedule</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="contributionAmount">Contribution amount (₹)</Label>
                  <Input
                    id="contributionAmount"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="5000"
                    aria-invalid={errors.contributionAmount ? true : undefined}
                    disabled={isSubmitting}
                    {...register("contributionAmount")}
                  />
                  {errors.contributionAmount && (
                    <p className="text-sm text-destructive">{errors.contributionAmount.message}</p>
                  )}
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="frequency">Frequency</Label>
                  <Controller
                    control={control}
                    name="frequency"
                    render={({ field }) => (
                      <Select
                        value={field.value}
                        onValueChange={(value) => field.onChange(value as ContributionFrequency)}
                      >
                        <SelectTrigger id="frequency" className="w-full" disabled={isSubmitting}>
                          <SelectValue>
                            {(value: ContributionFrequency) =>
                              contributionFrequencies.find((option) => option.value === value)?.label
                            }
                          </SelectValue>
                        </SelectTrigger>
                        <SelectContent>
                          {contributionFrequencies.map((option) => (
                            <SelectItem key={option.value} value={option.value}>
                              {option.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="startDate">Start date</Label>
                  <Input
                    id="startDate"
                    type="date"
                    aria-invalid={errors.startDate ? true : undefined}
                    disabled={isSubmitting}
                    {...register("startDate")}
                  />
                  {errors.startDate && (
                    <p className="text-sm text-destructive">{errors.startDate.message}</p>
                  )}
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="cycleCount">Number of cycles</Label>
                  <Input
                    id="cycleCount"
                    type="number"
                    step="1"
                    min="1"
                    max="120"
                    aria-invalid={errors.cycleCount ? true : undefined}
                    disabled={isSubmitting}
                    {...register("cycleCount")}
                  />
                  {errors.cycleCount && (
                    <p className="text-sm text-destructive">{errors.cycleCount.message}</p>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Membership &amp; payout</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="minimumMembers">Minimum members</Label>
                  <Input
                    id="minimumMembers"
                    type="number"
                    step="1"
                    min="2"
                    aria-invalid={errors.minimumMembers ? true : undefined}
                    disabled={isSubmitting}
                    {...register("minimumMembers")}
                  />
                  {errors.minimumMembers && (
                    <p className="text-sm text-destructive">{errors.minimumMembers.message}</p>
                  )}
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="maximumMembers">Maximum members</Label>
                  <Input
                    id="maximumMembers"
                    type="number"
                    step="1"
                    min="2"
                    max="500"
                    aria-invalid={errors.maximumMembers ? true : undefined}
                    disabled={isSubmitting}
                    {...register("maximumMembers")}
                  />
                  {errors.maximumMembers && (
                    <p className="text-sm text-destructive">{errors.maximumMembers.message}</p>
                  )}
                </div>
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="payoutMethod">Payout method</Label>
                <Controller
                  control={control}
                  name="payoutMethod"
                  render={({ field }) => (
                    <Select
                      value={field.value}
                      onValueChange={(value) => field.onChange(value as PayoutMethod)}
                    >
                      <SelectTrigger id="payoutMethod" className="w-full" disabled={isSubmitting}>
                        <SelectValue>
                          {(value: PayoutMethod) =>
                            payoutMethods.find((option) => option.value === value)?.label
                          }
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        {payoutMethods.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
              </div>

              <Controller
                control={control}
                name="partialPaymentsAllowed"
                render={({ field }) => (
                  <div className="flex items-center justify-between gap-2.5 rounded-lg border border-input px-3 py-2.5">
                    <Label htmlFor="partialPaymentsAllowed" className="font-normal">
                      Allow partial contributions
                    </Label>
                    <Switch
                      id="partialPaymentsAllowed"
                      checked={field.value}
                      onCheckedChange={(checked) => field.onChange(checked === true)}
                      disabled={isSubmitting}
                    />
                  </div>
                )}
              />
            </CardContent>
          </Card>

          <Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>
            {isSubmitting && <Loader2 className="size-4 animate-spin" />}
            Create group
          </Button>
        </form>
      </div>
    </PageContainer>
  );
}
