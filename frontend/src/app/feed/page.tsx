"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";

export default function FeedPage() {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, user, router]);

  if (loading || !user) {
    return null;
  }

  return (
    <main className="mx-auto w-full max-w-xl flex-1 p-8">
      <h1 className="text-2xl font-bold">Welcome, {user.username}</h1>
      <p className="mt-2 text-zinc-600 dark:text-zinc-400">
        Your feed will show up here once you follow people.
      </p>
    </main>
  );
}
