import type { NextConfig } from "next";

const securityHeaders = [
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "Permissions-Policy", value: "geolocation=(), microphone=(), camera=()" },
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
