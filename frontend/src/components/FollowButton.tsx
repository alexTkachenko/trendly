"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import { FollowStatus } from "@/lib/types";

export default function FollowButton({
  username,
  onFollowChange,
}: {
  username: string;
  onFollowChange?: (following: boolean) => void;
}) {
  const [following, setFollowing] = useState<boolean | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    let ignore = false;
    apiFetch<FollowStatus>(`/users/${username}/follow`)
      .then((status) => {
        if (!ignore) setFollowing(status.following);
      })
      .catch(() => {
        if (!ignore) setFollowing(false);
      });
    return () => {
      ignore = true;
    };
  }, [username]);

  async function toggle() {
    if (following === null) return;
    setPending(true);
    try {
      const status = await apiFetch<FollowStatus>(`/users/${username}/follow`, {
        method: following ? "DELETE" : "POST",
      });
      setFollowing(status.following);
      onFollowChange?.(status.following);
    } catch (err) {
      // leave state unchanged on failure; the ApiError message isn't shown here to keep this compact
      if (!(err instanceof ApiError)) throw err;
    } finally {
      setPending(false);
    }
  }

  if (following === null) return null;

  return (
    <button
      onClick={toggle}
      disabled={pending}
      className={
        following
          ? "rounded-full border border-black/[.15] px-4 py-1.5 text-sm disabled:opacity-50 dark:border-white/[.2]"
          : "rounded-full bg-foreground px-4 py-1.5 text-sm font-medium text-background disabled:opacity-50"
      }
    >
      {pending ? "…" : following ? "Following" : "Follow"}
    </button>
  );
}
