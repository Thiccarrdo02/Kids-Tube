import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
import { db } from '@/lib/db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function GET() {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  const [{ data: sources, error: se }, { data: videos, error: ve }] = await Promise.all([
    db.from('sources').select('*').order('created_at', { ascending: false }),
    db.from('videos').select('source_id'),
  ]);
  if (se) return NextResponse.json({ error: se.message }, { status: 500 });
  if (ve) return NextResponse.json({ error: ve.message }, { status: 500 });

  const counts = new Map<string, number>();
  for (const video of videos ?? []) {
    if (video.source_id) counts.set(video.source_id, (counts.get(video.source_id) ?? 0) + 1);
  }

  return NextResponse.json({
    items: (sources ?? []).map(source => ({
      ...source,
      video_count: counts.get(source.id) ?? 0,
    })),
  });
}
