import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
import { db } from '@/lib/db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function PATCH(req: Request, { params }: { params: { id: string } }) {
  const auth = await requireLogin();
  if (auth !== true) return auth;
  const { name } = await req.json().catch(() => ({}));
  if (typeof name !== 'string' || !name.trim()) {
    return NextResponse.json({ error: 'name required' }, { status: 400 });
  }
  const { error } = await db.from('categories').update({ name: name.trim() }).eq('id', params.id);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ ok: true });
}

export async function DELETE(_req: Request, { params }: { params: { id: string } }) {
  const auth = await requireLogin();
  if (auth !== true) return auth;
  // Videos pointing at this category get category_id = null (ON DELETE SET NULL).
  const { error } = await db.from('categories').delete().eq('id', params.id);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ ok: true });
}
