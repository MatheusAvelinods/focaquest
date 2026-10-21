"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";

import { api } from "@/lib/api";
import { useUser } from "@/lib/user";

const nav = [
  { href: "/", label: "Jornada", icon: "◎" },
  { href: "/mapa", label: "Mapa", icon: "▦" },
  { href: "/revisoes", label: "Revisões", icon: "↻" },
  { href: "/progresso", label: "Progresso", icon: "▲" },
  { href: "/preferencias", label: "Ajustes", icon: "⚙" },
];

/**
 * Estrutura comum das telas. No modo foco, a navegação é reduzida ao essencial para que
 * apenas a tarefa atual ocupe a atenção do estudante.
 */
export function AppShell({ children, minimal = false }: { children: ReactNode; minimal?: boolean }) {
  const pathname = usePathname();
  const { user, setUser } = useUser();
  const focus = user?.preferences.focusMode ?? false;
  const hideNav = minimal || focus;

  async function leaveFocus() {
    setUser(await api.updateMe({ focusMode: false }));
  }

  return (
    <div className="min-h-dvh">
      <a href="#conteudo" className="sr-only focus:not-sr-only focus:absolute focus:left-3 focus:top-3 focus:z-50 focus:rounded-lg focus:bg-surface focus:px-4 focus:py-2">
        Pular para o conteúdo
      </a>
      <header className="border-b border-line bg-surface/80 backdrop-blur">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3">
          <Link href="/" className="flex items-center gap-2 text-lg font-bold text-brand-strong">
            <span aria-hidden className="grid h-8 w-8 place-items-center rounded-lg bg-brand text-on-brand">F</span>
            FocaQuest
          </Link>
          {!hideNav && (
            <nav aria-label="Principal" className="hidden sm:block">
              <ul className="flex gap-1">
                {nav.map((item) => (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      aria-current={pathname === item.href ? "page" : undefined}
                      className="rounded-lg px-3 py-2 text-sm font-medium text-muted hover:bg-surface-muted hover:text-text aria-[current=page]:bg-brand-soft aria-[current=page]:text-brand-strong"
                    >
                      {item.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
          )}
          {focus && !minimal && (
            <button onClick={leaveFocus} className="rounded-lg px-3 py-2 text-sm font-medium text-muted hover:bg-surface-muted">
              Modo foco ativo · desativar
            </button>
          )}
        </div>
      </header>

      <main id="conteudo" className={`mx-auto max-w-5xl px-4 py-6 sm:py-8 ${hideNav ? "" : "pb-28 sm:pb-8"}`}>
        {children}
      </main>

      {!hideNav && (
        <nav aria-label="Principal (celular)" className="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-surface sm:hidden">
          <ul className="grid grid-cols-5">
            {nav.map((item) => (
              <li key={item.href}>
                <Link
                  href={item.href}
                  aria-current={pathname === item.href ? "page" : undefined}
                  className="flex min-h-14 flex-col items-center justify-center gap-0.5 text-xs font-medium text-muted aria-[current=page]:text-brand-strong"
                >
                  <span aria-hidden className="text-base">{item.icon}</span>
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </div>
  );
}
