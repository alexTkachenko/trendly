"use client";

import Link from "next/link";
import { useState } from "react";
import { apiFetch, ApiError, mediaUrl } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { Post, RepostStatus } from "@/lib/types";

export default function PostCard({ post, onDeleted }: { post: Post; onDeleted?: (id: number) => void }) {
  const { user } = useAuth();
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reposted, setReposted] = useState(post.repostedByMe);
  const [repostPending, setRepostPending] = useState(false);
  const isOwner = user?.username === post.author.username;

  async function handleDelete() {
    setError(null);
    setDeleting(true);
    try {
      await apiFetch<void>(`/posts/${post.id}`, { method: "DELETE" });
      onDeleted?.(post.id);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not delete post");
      setDeleting(false);
    }
  }

  async function toggleRepost() {
    setError(null);
    setRepostPending(true);
    try {
      const status = await apiFetch<RepostStatus>(`/posts/${post.id}/repost`, {
        method: reposted ? "DELETE" : "POST",
      });
      setReposted(status.reposted);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not update repost");
    } finally {
      setRepostPending(false);
    }
  }

  return (
    <article className="flex flex-col gap-3 rounded-lg border border-black/[.08] p-4 dark:border-white/[.145]">
      {post.repostedBy && (
        <p className="text-xs text-zinc-500">
          🔁 {post.repostedBy.username === user?.username ? "You" : post.repostedBy.displayName || post.repostedBy.username}{" "}
          reposted
        </p>
      )}
      <div className="flex items-center justify-between">
        <Link href={`/${post.author.username}`} className="text-sm font-medium hover:underline">
          {post.author.displayName || post.author.username}
        </Link>
        {isOwner && (
          <button
            onClick={handleDelete}
            disabled={deleting}
            className="text-xs text-zinc-500 hover:text-red-600 disabled:opacity-50"
          >
            {deleting ? "Deleting…" : "Delete"}
          </button>
        )}
      </div>
      <p className="whitespace-pre-wrap text-sm">{post.textContent}</p>
      {post.imageUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={mediaUrl(post.imageUrl)!} alt="" className="max-h-96 w-full rounded-md object-cover" />
      )}
      {error && <p className="text-sm text-red-600">{error}</p>}
      <div className="flex items-center justify-between">
        <Link href={`/posts/${post.id}`} className="w-fit text-xs text-zinc-500 hover:underline">
          <time dateTime={post.createdAt}>{new Date(post.createdAt).toLocaleString()}</time>
        </Link>
        <button
          onClick={toggleRepost}
          disabled={repostPending}
          className={
            reposted
              ? "text-xs font-medium text-green-600 disabled:opacity-50"
              : "text-xs text-zinc-500 hover:text-zinc-800 disabled:opacity-50 dark:hover:text-zinc-300"
          }
        >
          {repostPending ? "…" : reposted ? "🔁 Reposted" : "🔁 Repost"}
        </button>
      </div>
    </article>
  );
}
