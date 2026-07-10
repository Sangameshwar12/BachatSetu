"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { closeDraw, conductDraw, getDraw } from "@/services/draw-service";
import type { CloseDrawRequest } from "@/types/draw";

export function useDraw(drawId: string | null | undefined) {
  return useQuery({
    queryKey: ["draw", drawId] as const,
    queryFn: () => getDraw(drawId as string),
    enabled: Boolean(drawId),
  });
}

export function useConductDraw(drawId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => conductDraw(drawId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["draw", drawId] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "organizer"] });
    },
  });
}

export function useCloseDraw(drawId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CloseDrawRequest) => closeDraw(drawId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["draw", drawId] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "organizer"] });
    },
  });
}
