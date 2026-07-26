"use client";

import { ChangeEvent, FormEvent, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import { Post } from "@/lib/types";

export default function PostComposer({ onCreated }: { onCreated: (post: Post) => void }) {
  const [text, setText] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function handleImageChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0] ?? null;
    setImage(file);
    setPreviewUrl(file ? URL.createObjectURL(file) : null);
  }

  function clearImage() {
    setImage(null);
    setPreviewUrl(null);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!text.trim()) return;
    setError(null);
    setSubmitting(true);
    try {
      const formData = new FormData();
      formData.append("text", text);
      if (image) formData.append("image", image);

      const post = await apiFetch<Post>("/posts", { method: "POST", body: formData });
      setText("");
      clearImage();
      onCreated(post);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3 rounded-lg border border-black/[.08] p-4 dark:border-white/[.145]">
      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="What's on your mind?"
        rows={3}
        className="resize-none rounded-md border border-black/[.15] px-3 py-2 text-sm dark:border-white/[.2] dark:bg-transparent"
      />
      {previewUrl && (
        <div className="relative w-fit">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={previewUrl} alt="Preview" className="max-h-64 rounded-md" />
          <button
            type="button"
            onClick={clearImage}
            className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-black text-xs text-white"
          >
            ×
          </button>
        </div>
      )}
      <div className="flex items-center justify-between">
        <input type="file" accept="image/png,image/jpeg,image/webp,image/gif" onChange={handleImageChange} className="text-sm" />
        <button
          type="submit"
          disabled={submitting || !text.trim()}
          className="rounded-full bg-foreground px-4 py-2 text-sm font-medium text-background transition-colors hover:bg-[#383838] disabled:opacity-50 dark:hover:bg-[#ccc]"
        >
          {submitting ? "Posting…" : "Post"}
        </button>
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
    </form>
  );
}
