"use client";

import { FormEvent, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import { Comment } from "@/lib/types";

export default function CommentForm({ postId, onCreated }: { postId: number; onCreated: (comment: Comment) => void }) {
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!content.trim()) return;
    setError(null);
    setSubmitting(true);
    try {
      const comment = await apiFetch<Comment>(`/posts/${postId}/comments`, {
        method: "POST",
        body: JSON.stringify({ content }),
      });
      setContent("");
      onCreated(comment);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not post comment");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-2">
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="Write a comment…"
        rows={2}
        className="resize-none rounded-md border border-black/[.15] px-3 py-2 text-sm dark:border-white/[.2] dark:bg-transparent"
      />
      <div className="flex items-center justify-between">
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button
          type="submit"
          disabled={submitting || !content.trim()}
          className="ml-auto rounded-full bg-foreground px-4 py-1.5 text-sm font-medium text-background disabled:opacity-50"
        >
          {submitting ? "Posting…" : "Comment"}
        </button>
      </div>
    </form>
  );
}
