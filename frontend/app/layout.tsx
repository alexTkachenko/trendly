import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Trendly",
  description: "Trendly — a social network for fashion discovery.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full">
      <body className="min-h-full bg-zinc-50 text-zinc-900 antialiased">
        {children}
      </body>
    </html>
  );
}
