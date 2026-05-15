import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
import { db } from '@/lib/db';
import { hasDbUrl, runSql } from '@/lib/pg';
import { SCHEMA_SQL } from '@/lib/schema';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

// GET -- reports whether setup is needed and which path is available.
export async function GET() {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  const ready = await tableExists();
  return NextResponse.json({
    ready,
    autoSetupAvailable: hasDbUrl(),
    schemaSql: SCHEMA_SQL,
  });
}

// POST -- runs the schema SQL via direct PG connection.
export async function POST() {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  if (!hasDbUrl()) {
    return NextResponse.json(
      {
        error:
          'SUPABASE_DB_URL is not set. Either add it to your Vercel env vars (Settings -> Environment Variables, then redeploy) OR copy the SQL from this page into Supabase -> SQL Editor and run it manually.',
        schemaSql: SCHEMA_SQL,
      },
      { status: 412 },
    );
  }

  try {
    await runSql(SCHEMA_SQL);
  } catch (e: any) {
    return NextResponse.json(
      { error: `Database init failed: ${e.message ?? String(e)}` },
      { status: 500 },
    );
  }

  // PostgREST schema cache can take a moment to reload even after the
  // NOTIFY in the setup SQL. Poll briefly before reporting failure.
  for (let i = 0; i < 6; i++) {
    if (await tableExists()) return NextResponse.json({ ok: true });
    await new Promise(r => setTimeout(r, 500));
  }
  return NextResponse.json(
    {
      error:
        'Setup script ran but Supabase still reports the categories table as missing. Wait ~30s and click Recheck.',
    },
    { status: 500 },
  );
}

// Probes the public.categories table with a no-op count. Returns false if
// the table doesn't exist (PostgREST error code 42P01 / PGRST205).
async function tableExists(): Promise<boolean> {
  const { error } = await db
    .from('categories')
    .select('id', { count: 'exact', head: true });
  return !error;
}
