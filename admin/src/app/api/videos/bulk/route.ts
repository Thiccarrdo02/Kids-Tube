import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
import { db } from '@/lib/db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

function cleanIds(ids: unknown): string[] {
  return Array.isArray(ids)
    ? ids.filter((id): id is string => typeof id === 'string' && id.trim().length > 0)
    : [];
}

export async function PATCH(req: Request) {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  const { ids, categoryId } = await req.json().catch(() => ({}));
  const clean = cleanIds(ids);
  if (clean.length === 0) return NextResponse.json({ error: 'ids required' }, { status: 400 });
  if (typeof categoryId !== 'string') {
    return NextResponse.json({ error: 'categoryId required' }, { status: 400 });
  }

  const { error } = await db.from('videos').update({ category_id: categoryId || null }).in('video_id', clean);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ ok: true, updated: clean.length });
}

export async function DELETE(req: Request) {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  const { ids } = await req.json().catch(() => ({}));
  const clean = cleanIds(ids);
  if (clean.length === 0) return NextResponse.json({ error: 'ids required' }, { status: 400 });

  const { error } = await db.from('videos').delete().in('video_id', clean);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ ok: true, deleted: clean.length });
}
