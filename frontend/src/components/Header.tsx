"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";

export default function Header() {
  const { user, loading, logout } = useAuth();
  const router = useRouter();

  function handleLogout() {
    logout();
    router.push("/login");
  }

  return (
    <header className="flex items-center justify-between border-b border-black/[.08] px-6 py-4 dark:border-white/[.145]">
      <Link href={user ? "/feed" : "/"} className="text-lg font-bold tracking-tight">
        Trendly
      </Link>
      {!loading && (
        <nav className="flex items-center gap-4 text-sm">
          {user ? (
            <>
              <Link href={`/${user.username}`} className="font-medium hover:underline">
                {user.username}
              </Link>
              <button
                onClick={handleLogout}
                className="text-zinc-500 hover:text-zinc-800 dark:hover:text-zinc-300"
              >
                Log out
              </button>
            </>
          ) : (
            <>
              <Link href="/login" className="hover:underline">
                Log in
              </Link>
              <Link href="/register" className="hover:underline">
                Register
              </Link>
            </>
          )}
        </nav>
      )}
    </header>
  );
}
