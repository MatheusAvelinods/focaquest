"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Button, Card, ErrorMessage, Loading } from "@/components/ui";
import { api, ApiError } from "@/lib/api";
import { missionTypeHint, missionTypeLabel } from "@/lib/labels";
import type { Exam, MissionType, Role } from "@/lib/types";
import { UserProvider } from "@/lib/user";

const missionTypes: MissionType[] = ["MICRO", "STANDARD", "DEEP"];

function Onboarding() {
  const router = useRouter();
  const [exams, setExams] = useState<Exam[] | null>(null);
  const [roles, setRoles] = useState<Role[]>([]);
  const [examId, setExamId] = useState<string>("");
  const [roleId, setRoleId] = useState<string>("");
  const [missionType, setMissionType] = useState<MissionType>("STANDARD");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.exams().then((list) => {
      setExams(list);
      if (list.length === 1) setExamId(list[0].id);
    }, () => setError("Não foi possível carregar os concursos."));
  }, []);

  useEffect(() => {
    if (!examId) return;
    api.roles(examId).then((list) => {
      setRoles(list);
      if (list.length === 1) setRoleId(list[0].id);
    }, () => setError("Não foi possível carregar os cargos."));
  }, [examId]);

  async function finish() {
    setBusy(true);
    try {
      await api.updateMe({ selectedRoleId: roleId, preferredMissionType: missionType });
      router.replace("/");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não foi possível salvar.");
      setBusy(false);
    }
  }

  if (!exams) return error ? <ErrorMessage message={error} /> : <Loading />;

  const radio = "flex cursor-pointer items-start gap-3 rounded-xl border border-line p-4 has-[:checked]:border-brand has-[:checked]:bg-brand-soft";

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <header>
        <h1 className="text-2xl font-bold sm:text-3xl">Vamos preparar sua jornada</h1>
        <p className="mt-1 text-muted">Três escolhas rápidas. Você pode mudar tudo depois.</p>
      </header>

      <Card>
        <fieldset>
          <legend className="text-lg font-semibold">1. Concurso</legend>
          <div className="mt-3 space-y-2">
            {exams.map((e) => (
              <label key={e.id} className={radio}>
                <input type="radio" name="exam" value={e.id} checked={examId === e.id} onChange={() => setExamId(e.id)} className="mt-1.5 h-4 w-4 accent-[var(--brand)]" />
                <span>
                  <span className="block font-medium">{e.name}</span>
                  <span className="text-sm text-muted">Banca {e.board}</span>
                </span>
              </label>
            ))}
          </div>
        </fieldset>
      </Card>

      {examId && (
        <Card>
          <fieldset>
            <legend className="text-lg font-semibold">2. Cargo</legend>
            <div className="mt-3 space-y-2">
              {roles.map((r) => (
                <label key={r.id} className={radio}>
                  <input type="radio" name="role" value={r.id} checked={roleId === r.id} onChange={() => setRoleId(r.id)} className="mt-1.5 h-4 w-4 accent-[var(--brand)]" />
                  <span>
                    <span className="block font-medium">{r.name}</span>
                    {r.description && <span className="text-sm text-muted">{r.description}</span>}
                  </span>
                </label>
              ))}
            </div>
          </fieldset>
        </Card>
      )}

      {roleId && (
        <Card>
          <fieldset>
            <legend className="text-lg font-semibold">3. Quanto tempo você costuma ter por sessão?</legend>
            <p className="mt-1 text-sm text-muted">É só uma referência para sugerir missões — nunca uma obrigação.</p>
            <div className="mt-3 grid gap-2 sm:grid-cols-3">
              {missionTypes.map((t) => (
                <label key={t} className={radio}>
                  <input type="radio" name="missionType" value={t} checked={missionType === t} onChange={() => setMissionType(t)} className="mt-1.5 h-4 w-4 accent-[var(--brand)]" />
                  <span>
                    <span className="block font-medium">{missionTypeLabel[t]}</span>
                    <span className="text-sm text-muted">{missionTypeHint[t]}</span>
                  </span>
                </label>
              ))}
            </div>
          </fieldset>
        </Card>
      )}

      {error && <ErrorMessage message={error} />}
      <Button onClick={finish} disabled={!roleId || busy} className="w-full sm:w-auto">
        {busy ? "Salvando…" : "Ver minha primeira missão"}
      </Button>
    </div>
  );
}

export default function OnboardingPage() {
  return (
    <UserProvider>
      <main id="conteudo" className="px-4 py-8 sm:py-12">
        <Onboarding />
      </main>
    </UserProvider>
  );
}
