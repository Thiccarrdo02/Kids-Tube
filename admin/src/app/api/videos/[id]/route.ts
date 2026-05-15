import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
import { db } from '@/lib/db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

// PATCH /api/videos/[id]  body: { categoryId }
export async function PATCH(req: Request, { params }: { params: { id: string } }) {
  const auth = await requireLogin();
  if (auth !== true) return auth;
  const { categoryId } = await req.json().catch(() => ({}));
  if (typeof categoryId !== 'string') {
    return NextResponse.json({ error: 'categoryId required' }, { status: 400 });
  }
  const { error } = await db.from('videos').update({ category_id: categoryId }).eq('video_id', params.id);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ ok: true });
}

export async function DELETE(_req: Request, { params }: { params: { id: string } }) {
  const auth = await requireLogin();
  if (auth !== true) return auth;
  const { error } = await db.from('videos').delete().eq('video_id', params.id);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ ok: true });
}
