import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
import { db } from '@/lib/db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function GET() {
  const auth = await requireLogin();
  if (auth !== true) return auth;
  const { data, error } = await db.from('categories').select('*').order('sort_order');
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ items: data ?? [] });
}

export async function POST(req: Request) {
  const auth = await requireLogin();
  if (auth !== true) return auth;
  const { name } = await req.json().catch(() => ({}));
  if (typeof name !== 'string' || !name.trim()) {
    return NextResponse.json({ error: 'name required' }, { status: 400 });
  }
  const { data, error } = await db
    .from('categories')
    .insert({ name: name.trim(), sort_order: 50 })
    .select()
    .single();
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json(data);
}
