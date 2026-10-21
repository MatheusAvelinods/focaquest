"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { Button, Card, ErrorMessage, Loading } from "@/components/ui";
import { api, ApiError } from "@/lib/api";
import { missionTypeHint, missionTypeLabel, strategyLabel } from "@/lib/labels";
import type { MissionType, Preferences, StudyStrategy, Subject } from "@/lib/types";
import { UserProvider, useUser } from "@/lib/user";

const strategies = Object.keys(strategyLabel) as StudyStrategy[];
const missionTypes: MissionType[] = ["MICRO", "STANDARD", "DEEP"];

function Toggle({ label, hint, checked, onChange }: { label: string; hint: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <label className="flex cursor-pointer items-start justify-between gap-4 py-3">
      <span>
        <span className="block font-medium">{label}</span>
        <span className="text-sm text-muted">{hint}</span>
      </span>
      <input
        type="checkbox"
        role="switch"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="mt-1 h-5 w-5 shrink-0 accent-[var(--brand)]"
      />
    </label>
  );
}

function Settings() {
  const router = useRouter();
  const { user, setUser } = useUser();
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (user?.selectedRoleId) api.syllabus(user.selectedRoleId).then((s) => setSubjects(s.subjects), () => undefined);
  }, [user?.selectedRoleId]);

  if (!user) return <Loading />;
  const prefs = user.preferences;

  async function save(patch: Partial<Preferences>) {
    if (!user) return;
    const previous = user;
    setError(null);
    setSaved(false);
    // Atualização otimista: o controle responde na hora e volta atrás se o servidor recusar.
    setUser({ ...user, preferences: { ...user.preferences, ...patch } });
    try {
      setUser(await api.updateMe(patch));
      setSaved(true);
    } catch (e) {
      setUser(previous);
      setError(e instanceof ApiError ? e.message : "Não foi possível salvar.");
    }
  }

  async function logout() {
    await api.logout().catch(() => undefined);
    router.replace("/entrar");
  }

  const radio = "flex cursor-pointer items-start gap-3 rounded-xl border border-line p-3 has-[:checked]:border-brand has-[:checked]:bg-brand-soft";

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold sm:text-3xl">Ajustes</h1>
        <p className="text-muted">Adapte o FocaQuest ao seu jeito de estudar. Nenhum diagnóstico é necessário.</p>
      </header>
      <div aria-live="polite" className="min-h-6 text-sm">
        {saved && <span className="text-success">Preferências salvas.</span>}
      </div>
      {error && <ErrorMessage message={error} />}

      <Card>
        <h2 className="text-lg font-semibold">Atenção e conforto</h2>
        <div className="mt-2 divide-y divide-line">
          <Toggle
            label="Modo foco"
            hint="Oculta informações periféricas e reduz a navegação: uma tarefa por vez."
            checked={prefs.focusMode}
            onChange={(v) => save({ focusMode: v })}
          />
          <Toggle
            label="Reduzir animações"
            hint="Desativa transições e efeitos não essenciais."
            checked={prefs.reduceMotion}
            onChange={(v) => save({ reduceMotion: v })}
          />
          <Toggle
            label="Mostrar cronômetro nas missões"
            hint="Opcional. A duração das missões é sempre indicativa."
            checked={prefs.showTimer}
            onChange={(v) => save({ showTimer: v })}
          />
        </div>
      </Card>

      <Card>
        <fieldset>
          <legend className="text-lg font-semibold">Duração preferida das missões</legend>
          <div className="mt-3 grid gap-2 sm:grid-cols-3">
            {missionTypes.map((t) => (
              <label key={t} className={radio}>
                <input type="radio" name="missionType" checked={prefs.preferredMissionType === t} onChange={() => save({ preferredMissionType: t })} className="mt-1.5 h-4 w-4 accent-[var(--brand)]" />
                <span>
                  <span className="block font-medium">{missionTypeLabel[t]}</span>
                  <span className="text-sm text-muted">{missionTypeHint[t]}</span>
                </span>
              </label>
            ))}
          </div>
        </fieldset>
      </Card>

      <Card>
        <fieldset>
          <legend className="text-lg font-semibold">Como priorizar os conteúdos</legend>
          <div className="mt-3 space-y-2">
            {strategies.map((s) => (
              <label key={s} className={radio}>
                <input
                  type="radio"
                  name="strategy"
                  checked={prefs.strategy === s}
                  onChange={() =>
                    s === "BY_SUBJECT" && !prefs.strategySubjectId && subjects[0]
                      ? save({ strategy: s, strategySubjectId: subjects[0].id })
                      : save({ strategy: s })
                  }
                  className="mt-1.5 h-4 w-4 accent-[var(--brand)]"
                />
                <span>
                  <span className="block font-medium">{strategyLabel[s].title}</span>
                  <span className="text-sm text-muted">{strategyLabel[s].hint}</span>
                </span>
              </label>
            ))}
          </div>
          {prefs.strategy === "BY_SUBJECT" && (
            <label className="mt-4 block">
              <span className="font-medium">Disciplina</span>
              <select
                value={prefs.strategySubjectId ?? ""}
                onChange={(e) => save({ strategySubjectId: e.target.value })}
                className="mt-1 block w-full rounded-xl border border-line bg-surface px-3 py-2.5"
              >
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </label>
          )}
        </fieldset>
      </Card>

      <Card>
        <h2 className="text-lg font-semibold">Conta</h2>
        <p className="mt-1 text-sm text-muted">{user.email}</p>
        <div className="mt-4 flex flex-wrap gap-3">
          <Button variant="secondary" onClick={() => router.push("/onboarding")}>
            Trocar concurso ou cargo
          </Button>
          <Button variant="ghost" onClick={logout}>
            Sair
          </Button>
        </div>
      </Card>
    </div>
  );
}

export default function PreferenciasPage() {
  return (
    <UserProvider>
      <AppShell>
        <Settings />
      </AppShell>
    </UserProvider>
  );
}
