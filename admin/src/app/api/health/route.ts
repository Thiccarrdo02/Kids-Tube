import { NextResponse } from 'next/server';
import { db } from '@/lib/db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function GET() {
  const startedAt = Date.now();
  const [{ count: videoCount, error: videoError }, { count: categoryCount, error: categoryError }] =
    await Promise.all([
      db.from('videos').select('video_id', { count: 'exact', head: true }),
      db.from('categories').select('id', { count: 'exact', head: true }),
    ]);

  const ok = !videoError && !categoryError;
  return NextResponse.json(
    {
      ok,
      checkedAt: new Date().toISOString(),
      latencyMs: Date.now() - startedAt,
      database: {
        ok,
        videoCount: videoCount ?? 0,
        categoryCount: categoryCount ?? 0,
        error: videoError?.message ?? categoryError?.message ?? null,
      },
    },
    { status: ok ? 200 : 500 },
  );
}
