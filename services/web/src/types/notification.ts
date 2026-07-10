/** Mirrors `notification.interfaces.rest.dto.NotificationResponse` exactly. */
export interface NotificationResponse {
  notificationId: string;
  tenantId: string;
  recipientUserId: string;
  destination: string;
  channel: string;
  category: string;
  subject: string | null;
  body: string;
  status: string;
  scheduledAt: string;
  createdAt: string;
  updatedAt: string;
  deliveredAt: string | null;
  failureReason: string | null;
  version: number;
}
