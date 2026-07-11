# syntax=docker/dockerfile:1
#
# Multi-stage production image for the BachatSetu web frontend (Next.js 16, React 19).
# Stage 1 installs dependencies. Stage 2 builds the standalone server bundle
# (`output: "standalone"` in next.config.ts). Stage 3 runs only that bundle on a
# minimal Node runtime as a non-root user. Compression and long-lived cache headers
# for hashed static assets are set in next.config.ts and apply to the standalone
# server directly; see docs/deployment/docker-guide.md for the optional Nginx edge
# layer used in front of this container in docker-compose.prod.yml.

FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci

FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

ENV NEXT_TELEMETRY_DISABLED=1
ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
RUN npm run build

FROM node:22-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

RUN addgroup -S nextjs && adduser -S -G nextjs nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nextjs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nextjs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider "http://127.0.0.1:${PORT}/" || exit 1

CMD ["node", "server.js"]
