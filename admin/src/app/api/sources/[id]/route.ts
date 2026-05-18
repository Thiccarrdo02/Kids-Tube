import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
import { db } from '@/lib/db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function DELETE(_req: Request, { params }: { params: { id: string } }) {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  const { error: videoError } = await db.from('videos').update({ source_id: null }).eq('source_id', params.id);
  if (videoError) return NextResponse.json({ error: videoError.message }, { status: 500 });

  const { error } = await db.from('sources').delete().eq('id', params.id);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ ok: true });
}
