"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { apiFetch, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { PageResponse, Post } from "@/lib/types";
import PostComposer from "@/components/PostComposer";
import PostCard from "@/components/PostCard";
import FollowButton from "@/components/FollowButton";

const PAGE_SIZE = 20;

export default function ProfilePage() {
  const { username } = useParams<{ username: string }>();
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();

  const [posts, setPosts] = useState<Post[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const isOwnProfile = user?.username === username;

  const loadPage = useCallback(
    async (pageToLoad: number) => {
      setLoading(true);
      setError(null);
      try {
        const data = await apiFetch<PageResponse<Post>>(
          `/users/${username}/posts?page=${pageToLoad}&size=${PAGE_SIZE}`
        );
        setPosts((prev) => (pageToLoad === 0 ? data.content : [...prev, ...data.content]));
        setPage(data.page);
        setTotalPages(data.totalPages);
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "Could not load posts");
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

  function handleCreated(post: Post) {
    setPosts((prev) => [post, ...prev]);
  }

  function handleDeleted(id: number) {
    setPosts((prev) => prev.filter((p) => p.id !== id));
  }

  if (authLoading || !user) {
    return null;
  }

  return (
    <main className="mx-auto flex w-full max-w-xl flex-1 flex-col gap-6 p-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{username}</h1>
        {!isOwnProfile && <FollowButton username={username} />}
      </div>

      {isOwnProfile && <PostComposer onCreated={handleCreated} />}

      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="flex flex-col gap-4">
        {posts.map((post) => (
          <PostCard key={post.id} post={post} onDeleted={handleDeleted} />
        ))}
      </div>

      {!loading && posts.length === 0 && !error && (
        <p className="text-sm text-zinc-500">No posts yet.</p>
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
