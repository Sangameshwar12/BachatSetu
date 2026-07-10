"use client";

import { useMutation } from "@tanstack/react-query";

import { sendBroadcast } from "@/services/platform-operations-service";
import type { BroadcastRequest } from "@/types/platform-operations";

export function useSendBroadcast() {
  return useMutation({
    mutationFn: (request: BroadcastRequest) => sendBroadcast(request),
  });
}
