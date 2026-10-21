import { proxy } from "@/lib/proxy";

export const dynamic = "force-dynamic";

type Ctx = { params: Promise<{ path: string[] }> };

async function handle(request: Request, { params }: Ctx) {
  return proxy(request, (await params).path);
}

export { handle as GET, handle as POST, handle as PUT, handle as PATCH, handle as DELETE };
