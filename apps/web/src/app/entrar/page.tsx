"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";

import { Button, Card, ErrorMessage } from "@/components/ui";
import { api, ApiError } from "@/lib/api";

export default function EntrarPage() {
  const router = useRouter();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const email = String(form.get("email") ?? "");
    const password = String(form.get("password") ?? "");
    setBusy(true);
    setError(null);
    setFields({});
    try {
      const me =
        mode === "login"
          ? await api.login(email, password)
          : await api.register(email, password, String(form.get("displayName") ?? ""));
      router.replace(me.selectedRoleId ? "/" : "/onboarding");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível continuar.");
      if (err instanceof ApiError && err.fields) setFields(err.fields);
    } finally {
      setBusy(false);
    }
  }

  const input = "mt-1 block w-full rounded-xl border border-line bg-surface px-4 py-3 text-base";

  return (
    <main id="conteudo" className="mx-auto flex min-h-dvh max-w-md flex-col justify-center px-4 py-10">
      <div className="mb-8 text-center">
        <span aria-hidden className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-brand text-2xl font-bold text-on-brand">
          F
        </span>
        <h1 className="text-3xl font-bold">FocaQuest</h1>
        <p className="mt-2 text-muted">Missões curtas de estudo, no seu ritmo, para o seu concurso.</p>
      </div>

      <Card>
        <h2 className="text-xl font-semibold">{mode === "login" ? "Entrar" : "Criar conta"}</h2>
        <form onSubmit={onSubmit} className="mt-4 space-y-4" noValidate>
          {mode === "register" && (
            <label className="block">
              <span className="font-medium">Como podemos chamar você?</span>
              <input name="displayName" required maxLength={100} autoComplete="given-name" className={input} />
              {fields.displayName && <span className="text-sm text-danger">{fields.displayName}</span>}
            </label>
          )}
          <label className="block">
            <span className="font-medium">E-mail</span>
            <input name="email" type="email" required autoComplete="email" className={input} />
            {fields.email && <span className="text-sm text-danger">{fields.email}</span>}
          </label>
          <label className="block">
            <span className="font-medium">Senha</span>
            <input
              name="password"
              type="password"
              required
              minLength={8}
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              aria-describedby={mode === "register" ? "senha-dica" : undefined}
              className={input}
            />
            {mode === "register" && (
              <span id="senha-dica" className="text-sm text-muted">
                Pelo menos 8 caracteres.
              </span>
            )}
            {fields.password && <span className="block text-sm text-danger">{fields.password}</span>}
          </label>
          {error && <ErrorMessage message={error} />}
          <Button type="submit" disabled={busy} className="w-full">
            {busy ? "Aguarde…" : mode === "login" ? "Entrar" : "Criar conta e começar"}
          </Button>
        </form>
        <p className="mt-4 text-center text-sm text-muted">
          {mode === "login" ? "Ainda não tem conta?" : "Já tem conta?"}{" "}
          <button
            type="button"
            className="font-semibold text-brand underline-offset-2 hover:underline"
            onClick={() => {
              setMode(mode === "login" ? "register" : "login");
              setError(null);
              setFields({});
            }}
          >
            {mode === "login" ? "Criar conta" : "Entrar"}
          </button>
        </p>
      </Card>
      <p className="mt-6 text-center text-xs text-muted">
        Seus dados de estudo são usados apenas para personalizar sua preparação (LGPD).
      </p>
    </main>
  );
}
