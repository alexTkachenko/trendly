"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { apiFetch, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { DirectMessage, PageResponse } from "@/lib/types";

const PAGE_SIZE = 20;

export default function MessageThreadPage() {
  const { username } = useParams<{ username: string }>();
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();

  const [messages, setMessages] = useState<DirectMessage[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [content, setContent] = useState("");
  const [sending, setSending] = useState(false);

  const loadPage = useCallback(
    async (pageToLoad: number) => {
      setLoading(true);
      setError(null);
      try {
        const data = await apiFetch<PageResponse<DirectMessage>>(
          `/conversations/${username}/messages?page=${pageToLoad}&size=${PAGE_SIZE}`
        );
        setMessages((prev) => (pageToLoad === 0 ? data.content : [...prev, ...data.content]));
        setPage(data.page);
        setTotalPages(data.totalPages);
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "Could not load messages");
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

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!content.trim()) return;
    setSending(true);
    setError(null);
    try {
      const message = await apiFetch<DirectMessage>(`/conversations/${username}/messages`, {
        method: "POST",
        body: JSON.stringify({ content }),
      });
      setContent("");
      setMessages((prev) => [message, ...prev]);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not send message");
    } finally {
      setSending(false);
    }
  }

  if (authLoading || !user) {
    return null;
  }

  return (
    <main className="mx-auto flex w-full max-w-xl flex-1 flex-col gap-6 p-8">
      <div className="flex items-center gap-2">
        <Link href="/messages" className="text-sm text-zinc-500 hover:underline">
          ← Messages
        </Link>
        <h1 className="text-2xl font-bold">
          <Link href={`/${username}`} className="hover:underline">
            {username}
          </Link>
        </h1>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-2">
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="Write a message…"
          rows={2}
          className="resize-none rounded-md border border-black/[.15] px-3 py-2 text-sm dark:border-white/[.2] dark:bg-transparent"
        />
        <button
          type="submit"
          disabled={sending || !content.trim()}
          className="ml-auto rounded-full bg-foreground px-4 py-1.5 text-sm font-medium text-background disabled:opacity-50"
        >
          {sending ? "Sending…" : "Send"}
        </button>
      </form>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="flex flex-col gap-3">
        {messages.map((message) => (
          <div
            key={message.id}
            className={
              message.sender.username === user.username
                ? "self-end rounded-lg bg-foreground px-3 py-2 text-sm text-background"
                : "self-start rounded-lg border border-black/[.08] px-3 py-2 text-sm dark:border-white/[.145]"
            }
          >
            <p className="whitespace-pre-wrap">{message.content}</p>
            <time className="mt-1 block text-xs opacity-60" dateTime={message.createdAt}>
              {new Date(message.createdAt).toLocaleString()}
            </time>
          </div>
        ))}
      </div>

      {!loading && messages.length === 0 && !error && (
        <p className="text-sm text-zinc-500">No messages yet. Say hello!</p>
      )}

      {page + 1 < totalPages && (
        <button
          onClick={() => loadPage(page + 1)}
          disabled={loading}
          className="self-center rounded-full border border-black/[.15] px-4 py-2 text-sm disabled:opacity-50 dark:border-white/[.2]"
        >
          {loading ? "Loading…" : "Load older messages"}
        </button>
      )}
    </main>
  );
}
