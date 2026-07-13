import type { NextConfig } from "next";

// The browser calls the backend directly at this origin (see NEXT_PUBLIC_API_BASE_URL in
// docs/deployment/environment-variables-guide.md) — connect-src must allow it explicitly
// since CSP's default-src 'self' would otherwise block every API call.
const apiOrigin = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

// Inline <script type="application/ld+json"> (structured data) is exempt from script-src by
// the HTML spec's "script-supporting element" rules, so 'unsafe-inline' is not needed there.
// style-src does need 'unsafe-inline': components/ui/chart.tsx sets CSS custom properties via
// an inline <style> tag for per-series chart theming.
const contentSecurityPolicy = [
  "default-src 'self'",
  "script-src 'self' 'unsafe-inline'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: blob:",
  "font-src 'self' data:",
  `connect-src 'self'${apiOrigin ? ` ${apiOrigin}` : ""}`,
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
].join("; ");

const securityHeaders = [
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "Permissions-Policy", value: "geolocation=(), microphone=(), camera=()" },
  { key: "Content-Security-Policy", value: contentSecurityPolicy },
];

const nextConfig: NextConfig = {
  // Produces a self-contained `.next/standalone` server bundle (node server.js
  // plus only the node_modules it actually needs) so the production Docker
  // image doesn't have to ship the full node_modules tree.
  output: "standalone",
  compress: true,
  // Cache-Control for /_next/static/** is intentionally not set here: Next.js already
  // sends a long-lived immutable Cache-Control for hashed build assets in production, and
  // duplicating it here triggers a build-time warning about interfering with `next dev`
  // behavior. The Nginx edge (deploy/nginx/nginx.conf) sets the same header again for
  // defense-in-depth when the app is deployed behind it.
  async headers() {
    return [
      {
        source: "/:path*",
        headers: securityHeaders,
      },
    ];
  },
};

export default nextConfig;
