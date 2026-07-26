"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { apiFetch, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { PageResponse, UserSummary } from "@/lib/types";
import FollowButton from "@/components/FollowButton";

const PAGE_SIZE = 20;

export default function FollowingPage() {
  const { username } = useParams<{ username: string }>();
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();

  const [following, setFollowing] = useState<UserSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadPage = useCallback(
    async (pageToLoad: number) => {
      setLoading(true);
      setError(null);
      try {
        const data = await apiFetch<PageResponse<UserSummary>>(
          `/users/${username}/following?page=${pageToLoad}&size=${PAGE_SIZE}`
        );
        setFollowing((prev) => (pageToLoad === 0 ? data.content : [...prev, ...data.content]));
        setPage(data.page);
        setTotalPages(data.totalPages);
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "Could not load following list");
      } finally {
        setLoading(false);
      }
    },
    [username]
  );

  useEffect(() => {
    if (!authLoading && !user) {
      router.replace("/login");
      return;
    }
    if (user) {
      // Deferred so the fetch's setState calls land outside the effect's
      // synchronous body (react-hooks/set-state-in-effect).
      void Promise.resolve().then(() => loadPage(0));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authLoading, user, username]);

  function handleUnfollowed(targetUsername: string, isFollowing: boolean) {
    if (isFollowing) return;
    setFollowing((prev) => prev.filter((u) => u.username !== targetUsername));
  }

  if (authLoading || !user) {
    return null;
  }

  return (
    <main className="mx-auto flex w-full max-w-xl flex-1 flex-col gap-6 p-8">
      <div className="flex items-center gap-2">
        <Link href={`/${username}`} className="text-sm text-zinc-500 hover:underline">
          ← {username}
        </Link>
      </div>
      <h1 className="text-2xl font-bold">Following</h1>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <ul className="flex flex-col gap-3">
        {following.map((followee) => (
          <li
            key={followee.id}
            className="flex items-center justify-between rounded-lg border border-black/[.08] p-4 dark:border-white/[.145]"
          >
            <Link href={`/${followee.username}`} className="text-sm font-medium hover:underline">
              {followee.displayName || followee.username}
            </Link>
            <FollowButton
              username={followee.username}
              onFollowChange={(isFollowing) => handleUnfollowed(followee.username, isFollowing)}
            />
          </li>
        ))}
      </ul>

      {!loading && following.length === 0 && !error && (
        <p className="text-sm text-zinc-500">Not following anyone yet.</p>
      )}

      {page + 1 < totalPages && (
        <button
          onClick={() => loadPage(page + 1)}
          disabled={loading}
          className="self-center rounded-full border border-black/[.15] px-4 py-2 text-sm disabled:opacity-50 dark:border-white/[.2]"
        >
          {loading ? "Loading…" : "Load more"}
        </button>
      )}
    </main>
  );
}
