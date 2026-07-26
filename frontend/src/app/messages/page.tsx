"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { apiFetch, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { ConversationSummary, PageResponse } from "@/lib/types";

const PAGE_SIZE = 20;

export default function MessagesPage() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();

  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadPage = useCallback(async (pageToLoad: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetch<PageResponse<ConversationSummary>>(
        `/conversations?page=${pageToLoad}&size=${PAGE_SIZE}`
      );
      setConversations((prev) => (pageToLoad === 0 ? data.content : [...prev, ...data.content]));
      setPage(data.page);
      setTotalPages(data.totalPages);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not load conversations");
    } finally {
      setLoading(false);
    }
  }, []);

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
  }, [authLoading, user]);

  if (authLoading || !user) {
    return null;
  }

  return (
    <main className="mx-auto flex w-full max-w-xl flex-1 flex-col gap-6 p-8">
      <h1 className="text-2xl font-bold">Messages</h1>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="flex flex-col gap-3">
        {conversations.map((conversation) => (
          <Link
            key={conversation.otherUser.username}
            href={`/messages/${conversation.otherUser.username}`}
            className="flex flex-col gap-1 rounded-lg border border-black/[.08] p-4 hover:bg-black/[.02] dark:border-white/[.145] dark:hover:bg-white/[.03]"
          >
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">
                {conversation.otherUser.displayName || conversation.otherUser.username}
              </span>
              <time className="text-xs text-zinc-500" dateTime={conversation.lastMessageAt}>
                {new Date(conversation.lastMessageAt).toLocaleString()}
              </time>
            </div>
            <p className="truncate text-sm text-zinc-500">
              {conversation.lastMessageFromMe ? "You: " : ""}
              {conversation.lastMessageContent}
            </p>
          </Link>
        ))}
      </div>

      {!loading && conversations.length === 0 && !error && (
        <p className="text-sm text-zinc-500">
          No conversations yet. Visit a profile and send a message to start one.
        </p>
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
