import { apiBaseUrl } from "@/src/lib/config";

/**
 * Minimal landing route (FE-1.1 skeleton).
 *
 * Renders the build-time `NEXT_PUBLIC_API_BASE_URL` value to prove the
 * Docker build-arg -> Next.js inlined-env wiring works end to end. No API
 * client and no real app shell yet — those land in FE-1.2 / FE-1.3.
 */
export default function HomePage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 p-8 text-center">
      <h1 className="text-2xl font-semibold tracking-tight">Trendly</h1>
      <p className="text-sm text-zinc-600">
        Frontend skeleton is up. API base URL configured at build time:
      </p>
      <p
        data-testid="api-base-url"
        className="rounded bg-zinc-900 px-3 py-1 font-mono text-sm text-zinc-50"
      >
        {apiBaseUrl}
      </p>
    </main>
  );
}
