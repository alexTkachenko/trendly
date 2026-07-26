import Link from "next/link";

export default function Home() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6 p-8 text-center">
      <h1 className="text-4xl font-bold tracking-tight">Trendly</h1>
      <p className="max-w-md text-zinc-600 dark:text-zinc-400">
        Post, follow, and see what people you follow are sharing.
      </p>
      <div className="flex gap-4">
        <Link
          href="/login"
          className="rounded-full bg-foreground px-5 py-2.5 text-sm font-medium text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
        >
          Log in
        </Link>
        <Link
          href="/register"
          className="rounded-full border border-black/[.08] px-5 py-2.5 text-sm font-medium transition-colors hover:bg-black/[.04] dark:border-white/[.145] dark:hover:bg-[#1a1a1a]"
        >
          Register
        </Link>
      </div>
    </main>
  );
}
