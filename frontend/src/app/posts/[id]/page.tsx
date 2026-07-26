"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { apiFetch, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { Comment, PageResponse, Post } from "@/lib/types";
import PostCard from "@/components/PostCard";
import CommentForm from "@/components/CommentForm";
import CommentList from "@/components/CommentList";

const PAGE_SIZE = 20;

export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();

  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [commentsPage, setCommentsPage] = useState(0);
  const [commentsTotalPages, setCommentsTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadCommentsPage = useCallback(
    async (pageToLoad: number) => {
      const data = await apiFetch<PageResponse<Comment>>(`/posts/${id}/comments?page=${pageToLoad}&size=${PAGE_SIZE}`);
      setComments((prev) => (pageToLoad === 0 ? data.content : [...prev, ...data.content]));
      setCommentsPage(data.page);
      setCommentsTotalPages(data.totalPages);
    },
    [id]
  );

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [postData] = await Promise.all([apiFetch<Post>(`/posts/${id}`), loadCommentsPage(0)]);
      setPost(postData);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not load post");
    } finally {
      setLoading(false);
    }
  }, [id, loadCommentsPage]);

  useEffect(() => {
    if (!authLoading && !user) {
      router.replace("/login");
      return;
    }
    if (user) {
      // Deferred so the fetch's setState calls land outside the effect's
      // synchronous body (react-hooks/set-state-in-effect).
      void Promise.resolve().then(() => loadAll());
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authLoading, user, id]);

  function handleCommentCreated(comment: Comment) {
    setComments((prev) => [...prev, comment]);
  }

  function handlePostDeleted() {
    router.push("/feed");
  }

  if (authLoading || !user) {
    return null;
  }

  return (
    <main className="mx-auto flex w-full max-w-xl flex-1 flex-col gap-6 p-8">
      {error && <p className="text-sm text-red-600">{error}</p>}

      {loading && !post && <p className="text-sm text-zinc-500">Loading…</p>}

      {post && <PostCard post={post} onDeleted={handlePostDeleted} />}

      {post && (
        <div className="flex flex-col gap-4">
          <h2 className="text-lg font-semibold">Comments</h2>
          <CommentForm postId={post.id} onCreated={handleCommentCreated} />
          <CommentList comments={comments} />
          {commentsPage + 1 < commentsTotalPages && (
            <button
              onClick={() => loadCommentsPage(commentsPage + 1)}
              className="self-center rounded-full border border-black/[.15] px-4 py-2 text-sm dark:border-white/[.2]"
            >
              Load more comments
            </button>
          )}
        </div>
      )}
    </main>
  );
}
