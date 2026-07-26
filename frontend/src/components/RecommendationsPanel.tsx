"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { apiFetch } from "@/lib/api";
import { PageResponse, UserSummary } from "@/lib/types";
import FollowButton from "@/components/FollowButton";

export default function RecommendationsPanel() {
  const [users, setUsers] = useState<UserSummary[] | null>(null);

  useEffect(() => {
    let ignore = false;
    apiFetch<PageResponse<UserSummary>>("/recommendations?page=0&size=5")
      .then((data) => {
        if (!ignore) setUsers(data.content);
      })
      .catch(() => {
        if (!ignore) setUsers([]);
      });
    return () => {
      ignore = true;
    };
  }, []);

  function handleFollowed(username: string, following: boolean) {
    if (!following) return;
    setUsers((prev) => (prev ? prev.filter((u) => u.username !== username) : prev));
  }

  if (!users || users.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-black/[.08] p-4 dark:border-white/[.145]">
      <h2 className="text-sm font-semibold text-zinc-500">Who to follow</h2>
      <ul className="flex flex-col gap-3">
        {users.map((user) => (
          <li key={user.id} className="flex items-center justify-between">
            <Link href={`/${user.username}`} className="text-sm font-medium hover:underline">
              {user.displayName || user.username}
            </Link>
            <FollowButton username={user.username} onFollowChange={(following) => handleFollowed(user.username, following)} />
          </li>
        ))}
      </ul>
    </div>
  );
}
