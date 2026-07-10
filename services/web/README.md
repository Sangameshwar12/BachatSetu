# BachatSetu — Web

The web frontend for BachatSetu, a platform for running Bhishi and chit-fund savings groups:
OTP sign-in, group management, contributions and draws, receipts, and a platform admin portal.

## Tech stack

- **Next.js 16** (App Router, Turbopack) + **React 19** + **TypeScript** (strict)
- **TailwindCSS v4** + **shadcn/ui** (`base-nova` preset, backed by `@base-ui/react`, not Radix)
- **TanStack React Query v5** for all server state
- **react-hook-form** + **zod** for forms
- **recharts** (lazy-loaded) for admin analytics charts
- **sonner** for toasts, **next-themes** for dark mode

## Getting started

```bash
npm install
cp .env.example .env.local   # then edit NEXT_PUBLIC_API_BASE_URL if needed
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). The backend (`services/backend`) must be
running and reachable at `NEXT_PUBLIC_API_BASE_URL` for anything beyond the public marketing
pages to work — this app has no mock/offline data mode.

## Environment variables

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | No | `http://localhost:8080` | Base URL of the backend API. Must be reachable from the browser directly (CORS-enabled) — every request is made client-side. |

See `.env.example` for the template. There is no server-only secret in this app: it holds no API
keys and talks to the backend with a bearer JWT obtained at login, so every env var here is
intentionally `NEXT_PUBLIC_*`.

## Scripts

| Command | Purpose |
| --- | --- |
| `npm run dev` | Start the dev server (Turbopack, hot reload) |
| `npm run build` | Production build (also runs the TypeScript check) |
| `npm run start` | Serve the production build |
| `npm run lint` | ESLint (flat config, `eslint-config-next`) |

There is no automated test suite yet (see **Known limitations** below) — `lint` and `build` are
the current correctness gates and both must pass clean before merging.

## Architecture

- **App Router route groups** under `src/app/`: `(marketing)` (public site, its own header/footer),
  `(auth)` (login/signup/onboarding, chrome-free), `(dashboard)` (everything behind
  `ProtectedRoute`, shares the sidebar/topbar shell). A route group doesn't add a URL segment, so
  every dashboard page path is nested under a literal `dashboard/` folder inside `(dashboard)`.
- **Every route** follows a server `page.tsx` (metadata) + client `*-content.tsx` (the actual UI)
  + `loading.tsx` split, so metadata stays server-renderable while the interactive content stays a
  client component.
- **`src/features/<domain>/`** holds the client components per business area (e.g. `groups`,
  `organizer`, `admin`, `auth`, `landing`). **`src/components/`** holds cross-domain shared UI:
  `ui/` (shadcn primitives), `dashboard/` (shell chrome + shared widgets like `PaginationControls`,
  `StatusBadge`), `shared/` (`EmptyState`, `ErrorState`), `auth/` (`ProtectedRoute`, `RoleGuard`).
- **Data flow**: `src/services/*.ts` (thin axios wrappers, one per backend module) → `src/hooks/*`
  (TanStack Query hooks, one per resource) → feature components. Types in `src/types/*.ts` mirror
  backend REST DTOs field-for-field — nothing is invented client-side.
- **Auth**: the backend issues bearer JWTs only (no session cookie), so `AuthContext` persists the
  token pair in `localStorage` and a module-level `token-store.ts` bridges it into axios's request
  interceptor (which can't use React hooks). `ProtectedRoute` gates on authentication;
  `RoleGuard` (used for `/dashboard/admin`) additionally checks the JWT's decoded `roles` claim —
  a UX convenience only, since every admin endpoint independently enforces the same role
  server-side.
- **Honest-gap pattern**: wherever the backend doesn't yet expose enough data for a feature in the
  brief, the UI shows a real, labeled empty/placeholder state explaining the limitation instead of
  fabricating data. Search for `comingSoon`, `EmptyState`, and inline `Alert` disclosures.

### Folder structure

```
src/
  app/            Route segments (see Architecture above)
  components/     Shared UI: ui/, dashboard/, shared/, auth/, layout/, pwa/, monitoring/
  constants/      Static config: site, auth, dashboard nav, notification metadata
  contexts/       AppProviders composition root: auth, theme, React Query, tooltip
  features/       Per-domain client components (groups, organizer, admin, auth, landing, ...)
  hooks/          React Query hooks, one file per resource
  lib/            Cross-cutting utilities: env, jwt decode, logger, token-store, cn
  services/       Axios wrappers per backend module
  types/          TypeScript types mirroring backend REST DTOs
  utils/          Pure formatting helpers (currency, dates, bytes)
```

## Production readiness

- **SEO**: per-page metadata + OpenGraph/Twitter cards, canonical URLs, JSON-LD on the landing
  page, `app/sitemap.ts`, `app/robots.ts` (dashboard routes disallowed — they require auth and
  have no public content to index).
- **PWA**: `app/manifest.ts`, an SVG app icon, `theme-color`, and a minimal offline-fallback
  service worker (`public/sw.js` + `/offline` page) — network requests are never cached or served
  stale, only a friendly fallback is shown if a navigation request fails outright.
- **Monitoring**: `src/lib/logger.ts` is the single seam for wiring a real provider (Sentry,
  Datadog, ...) later — every error boundary and the global `unhandledrejection`/`window.onerror`
  listener (`GlobalErrorListener`) already route through it instead of calling `console.*`
  directly.
- **Security**: JWTs live only in memory + `localStorage` (no cookies, nothing else reads them);
  a 401 clears the session, toasts "session expired", and redirects to `/login`; `RoleGuard`
  hides/blocks the admin portal client-side ahead of the real server-side check; file uploads
  validate type and size client-side before hitting the network.
- **Accessibility**: a skip-to-content link, labeled landmarks (`nav[aria-label]`), `aria-current`
  on the active nav item, and Base UI's built-in focus trapping in every dialog/alert-dialog.

## Known limitations

- **No automated tests.** Correctness is currently verified by `lint`, `build`, and manual
  browser-preview checks per sprint. Adding component/integration tests (Vitest + Testing Library)
  is the top item before a wider launch.
- **No custom "Add to Home Screen" / update-available UI.** The manifest + service worker satisfy
  installability, but rely on the browser's native install prompt; a custom prompt and an
  update-available toast are not built.
- **Several backend gaps are UI-visible by design** (not bugs): no cross-tenant audit search, no
  server-side text search on the admin groups list, no group contribution-rules editing after
  creation. Each is disclosed inline where it appears — see the relevant sprint report for the
  full list per screen.
- Privacy Policy and Terms of Service (`/privacy`, `/terms`) are early-access placeholder copy,
  not reviewed legal text — flagged as such on both pages.

## Deployment

This is a standard Next.js app (no custom server, no `output: "export"`), deployable to Vercel or
any Node host that runs `npm run build && npm run start`. Set `NEXT_PUBLIC_API_BASE_URL` to the
production backend URL at build time (it's inlined into the client bundle, like all
`NEXT_PUBLIC_*` vars) and ensure that backend's CORS config allows the deployed origin.
