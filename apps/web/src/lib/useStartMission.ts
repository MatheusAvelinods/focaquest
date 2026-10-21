"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { api, ApiError, missionToRequest } from "./api";
import type { Mission, StartSessionRequest } from "./types";

/** Inicia (ou retoma) uma missão e leva o estudante direto para ela. */
export function useStartMission() {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function start(mission: Mission | StartSessionRequest) {
    setBusy(true);
    setError(null);
    try {
      const req = "reason" in mission ? missionToRequest(mission) : mission;
      const session = await api.startSession(req);
      router.push(`/missao/${session.id}`);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não foi possível iniciar a missão.");
      setBusy(false);
    }
  }

  return { start, busy, error };
}
