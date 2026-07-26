import type { NextConfig } from "next";

/**
 * `output: 'standalone'` is required by frontend/Dockerfile's runtime
 * stage, which copies `.next/standalone` + `.next/static` + `public/` into
 * the final image and runs `node server.js` (no `next start`, no full
 * `node_modules`). See tasks/devops/OPS-1.3.md and
 * tasks/frontend/FE-1.1.md.
 */
const nextConfig: NextConfig = {
  output: "standalone",
  reactStrictMode: true,
};

export default nextConfig;
