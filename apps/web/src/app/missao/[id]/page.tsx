"use client";

import { use, useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { MissionPlayer } from "@/components/MissionPlayer";
import { ErrorMessage, Loading } from "@/components/ui";
import { api, ApiError } from "@/lib/api";
import type { Session } from "@/lib/types";
import { UserProvider, useUser } from "@/lib/user";

function Mission({ id }: { id: string }) {
  const { user } = useUser();
  const [session, setSession] = useState<Session | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        let s = await api.session(id);
        // Abrir uma missão pausada é retomá-la.
        if (s.status === "PAUSED") s = await api.updateSession(id, { action: "RESUME" });
        setSession(s);
      } catch (e) {
        setError(e instanceof ApiError ? e.message : "Não foi possível abrir a missão.");
      }
    })();
  }, [id]);

  if (error) return <ErrorMessage message={error} />;
  if (!session || !user) return <Loading label="Abrindo a missão" />;
  return <MissionPlayer initial={session} focusMode={user.preferences.focusMode} showTimer={user.preferences.showTimer} />;
}

export default function MissionPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return (
    <UserProvider>
      <AppShell minimal>
        <Mission id={id} />
      </AppShell>
    </UserProvider>
  );
}
