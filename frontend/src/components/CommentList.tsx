import Link from "next/link";
import { Comment } from "@/lib/types";

export default function CommentList({ comments }: { comments: Comment[] }) {
  if (comments.length === 0) {
    return <p className="text-sm text-zinc-500">No comments yet.</p>;
  }

  return (
    <ul className="flex flex-col gap-3">
      {comments.map((comment) => (
        <li key={comment.id} className="rounded-md border border-black/[.08] p-3 dark:border-white/[.145]">
          <div className="flex items-center justify-between">
            <Link href={`/${comment.author.username}`} className="text-sm font-medium hover:underline">
              {comment.author.displayName || comment.author.username}
            </Link>
            <time className="text-xs text-zinc-500" dateTime={comment.createdAt}>
              {new Date(comment.createdAt).toLocaleString()}
            </time>
          </div>
          <p className="mt-1 whitespace-pre-wrap text-sm">{comment.content}</p>
        </li>
      ))}
    </ul>
  );
}
