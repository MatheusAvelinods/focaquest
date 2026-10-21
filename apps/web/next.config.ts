import type { NextConfig } from "next";

// /api/* é encaminhado à API por src/app/api/[...path]/route.ts, com o endereço lido em
// tempo de execução (FOCAQUEST_API_URL).
const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "X-Frame-Options", value: "DENY" },
        ],
      },
    ];
  },
};

export default nextConfig;
