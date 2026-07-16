/** Builds the WhatsApp share message for a group invitation, in the exact requested format. */
export function buildInvitationShareMessage(params: {
  groupName: string;
  inviteCode: string;
  inviteLink: string;
}): string {
  return `Join my BachatSetu Savings Group.

Group:
${params.groupName}

Invite Code:
${params.inviteCode}

Join here:

${params.inviteLink}`;
}

/**
 * Opens WhatsApp with the given message pre-filled — the native app on mobile (via the
 * official `wa.me` universal link, which redirects into the installed app), WhatsApp Web on
 * desktop.
 */
export function shareViaWhatsApp(message: string): void {
  const isMobile = /android|iphone|ipad|ipod/i.test(navigator.userAgent);
  const encoded = encodeURIComponent(message);
  const url = isMobile
    ? `https://wa.me/?text=${encoded}`
    : `https://web.whatsapp.com/send?text=${encoded}`;
  window.open(url, "_blank", "noopener,noreferrer");
}
