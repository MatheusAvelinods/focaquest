"use client";

import { useRouter } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";

import { api, ApiError } from "./api";
import type { Me } from "./types";

interface UserContextValue {
  user: Me | null;
  loading: boolean;
  refresh: () => Promise<void>;
  setUser: (u: Me) => void;
}

const UserContext = createContext<UserContextValue | null>(null);

/** Carrega o estudante autenticado e aplica as preferências de atenção ao documento. */
export function UserProvider({ children, requireAuth = true }: { children: ReactNode; requireAuth?: boolean }) {
  const router = useRouter();
  const [user, setUser] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      setUser(await api.me());
    } catch (e) {
      if (e instanceof ApiError && e.status === 401 && requireAuth) {
        router.replace("/entrar");
      }
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, [requireAuth, router]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  useEffect(() => {
    const root = document.documentElement;
    root.dataset.reduceMotion = String(user?.preferences.reduceMotion ?? false);
    root.dataset.focusMode = String(user?.preferences.focusMode ?? false);
  }, [user]);

  return <UserContext.Provider value={{ user, loading, refresh, setUser }}>{children}</UserContext.Provider>;
}

export function useUser(): UserContextValue {
  const ctx = useContext(UserContext);
  if (!ctx) throw new Error("useUser deve ser usado dentro de <UserProvider>");
  return ctx;
}
