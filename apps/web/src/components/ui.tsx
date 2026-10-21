import Link from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";

import { topicStateLabel } from "@/lib/labels";
import type { TopicState } from "@/lib/types";

type Variant = "primary" | "secondary" | "ghost";

const variants: Record<Variant, string> = {
  primary: "bg-brand text-on-brand hover:bg-brand-strong shadow-sm",
  secondary: "bg-surface text-text border border-line hover:bg-surface-muted",
  ghost: "text-brand hover:bg-brand-soft",
};

const base =
  "inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-5 py-2.5 font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60";

export function Button({
  variant = "primary",
  className = "",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return <button {...props} className={`${base} ${variants[variant]} ${className}`} />;
}

export function ButtonLink({
  href,
  variant = "primary",
  className = "",
  children,
}: {
  href: string;
  variant?: Variant;
  className?: string;
  children: ReactNode;
}) {
  return (
    <Link href={href} className={`${base} ${variants[variant]} ${className}`}>
      {children}
    </Link>
  );
}

export function Card({ children, className = "", as: As = "section", ...rest }: {
  children: ReactNode;
  className?: string;
  as?: "section" | "div" | "article";
  "aria-labelledby"?: string;
}) {
  return (
    <As {...rest} className={`rounded-2xl border border-line bg-surface p-5 sm:p-6 ${className}`}>
      {children}
    </As>
  );
}

export function ProgressBar({ value, label, className = "" }: { value: number; label: string; className?: string }) {
  const clamped = Math.max(0, Math.min(100, value));
  return (
    <div
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(clamped)}
      className={`h-2.5 w-full overflow-hidden rounded-full bg-surface-muted ${className}`}
    >
      <div className="h-full rounded-full bg-brand transition-[width] duration-500" style={{ width: `${clamped}%` }} />
    </div>
  );
}

const stateStyles: Record<TopicState, string> = {
  NOT_STARTED: "bg-surface-muted text-muted",
  LEARNING: "bg-info-soft text-info",
  NEEDS_REINFORCEMENT: "bg-warning-soft text-warning",
  IN_REVIEW: "bg-brand-soft text-brand-strong",
  MASTERED: "bg-success-soft text-success",
};

export function StateBadge({ state }: { state: TopicState }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${stateStyles[state]}`}>
      {topicStateLabel[state]}
    </span>
  );
}

export function Loading({ label = "Carregando…" }: { label?: string }) {
  return (
    <div role="status" aria-live="polite" className="flex flex-col gap-3 py-8">
      <span className="sr-only">{label}</span>
      <div aria-hidden className="h-6 w-1/2 animate-pulse rounded bg-surface-muted" />
      <div aria-hidden className="h-24 w-full animate-pulse rounded-2xl bg-surface-muted" />
      <div aria-hidden className="h-24 w-full animate-pulse rounded-2xl bg-surface-muted" />
    </div>
  );
}

export function ErrorMessage({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="rounded-2xl border border-danger/40 bg-danger-soft p-4 text-danger">
      <p className="font-medium">{message}</p>
      {onRetry && (
        <Button variant="secondary" className="mt-3" onClick={onRetry}>
          Tentar novamente
        </Button>
      )}
    </div>
  );
}
