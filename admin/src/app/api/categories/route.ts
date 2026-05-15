import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
import { db } from '@/lib/db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const DEFAULT_CATEGORIES = [
  { name: 'Islamic',     sort_order: 1 },
  { name: 'Educational', sort_order: 2 },
  { name: 'Stories',     sort_order: 3 },
  { name: 'Cartoons',    sort_order: 4 },
  { name: 'Other',       sort_order: 99 },
];

export async function GET() {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  let { data, error } = await db.from('categories').select('*').order('sort_order');
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });

  // First-run safety net: if the table is empty (the SQL seed never ran, or
  // someone wiped the rows), insert the default set on demand so the admin
  // UI is never stuck with an empty category dropdown.
  if (!data || data.length === 0) {
    const { error: seedErr } = await db
      .from('categories')
      .upsert(DEFAULT_CATEGORIES, { onConflict: 'name' });
    if (seedErr) return NextResponse.json({ error: seedErr.message }, { status: 500 });
    ({ data, error } = await db.from('categories').select('*').order('sort_order'));
    if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  }

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
