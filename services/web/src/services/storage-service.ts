import { apiClient } from "@/services/api-client";
import type { UploadFileResponse } from "@/types/auth";

/** `POST /api/v1/storage/files` — authenticated multipart upload; returns a reusable `fileId`. */
export async function uploadFile(file: File): Promise<UploadFileResponse> {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await apiClient.post<UploadFileResponse>("/api/v1/storage/files", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}
