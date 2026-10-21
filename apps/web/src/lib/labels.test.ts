import { describe, expect, it } from "vitest";

import { percent, relativeDay } from "./labels";

describe("relativeDay", () => {
  const now = new Date("2026-10-21T12:00:00");

  it("descreve datas próximas em linguagem natural", () => {
    expect(relativeDay("2026-10-21T20:00:00", now)).toBe("hoje");
    expect(relativeDay("2026-10-22T08:00:00", now)).toBe("amanhã");
    expect(relativeDay("2026-10-20T08:00:00", now)).toBe("ontem");
    expect(relativeDay("2026-10-28T08:00:00", now)).toBe("em 7 dias");
    expect(relativeDay("2026-10-18T08:00:00", now)).toBe("há 3 dias");
  });
});

describe("percent", () => {
  it("arredonda para inteiro", () => {
    expect(percent(0.666)).toBe("67%");
    expect(percent(1)).toBe("100%");
    expect(percent(0)).toBe("0%");
  });
});
