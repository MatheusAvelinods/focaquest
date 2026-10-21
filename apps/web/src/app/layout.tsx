import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";

import { ServiceWorker } from "@/components/ServiceWorker";

import "./globals.css";

export const metadata: Metadata = {
  title: { default: "FocaQuest", template: "%s · FocaQuest" },
  description: "Aprendizagem adaptativa para concursos públicos, em missões curtas.",
  manifest: "/manifest.webmanifest",
  icons: { icon: "/icon.svg", apple: "/icon.svg" },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#0f6b5c" },
    { media: "(prefers-color-scheme: dark)", color: "#121615" },
  ],
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-BR">
      <body className="antialiased">
        {children}
        <ServiceWorker />
      </body>
    </html>
  );
}
