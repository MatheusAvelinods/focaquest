import type { NextConfig } from "next";

const apiUrl = process.env.FOCAQUEST_API_URL ?? "http://localhost:8080";

// O navegador fala apenas com a origem do frontend; /api é repassado ao backend.
// Assim o cookie de sessão (httpOnly, SameSite=Strict) funciona sem CORS.
const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${apiUrl}/api/:path*` }];
  },
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
