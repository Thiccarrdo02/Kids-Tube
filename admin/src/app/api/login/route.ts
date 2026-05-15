import { NextResponse } from 'next/server';
import { getSession } from '@/lib/auth';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function POST(req: Request) {
  const { password } = await req.json().catch(() => ({}));
  const expected = process.env.ADMIN_PASSWORD;
  if (!expected) {
    return NextResponse.json({ error: 'ADMIN_PASSWORD not configured' }, { status: 500 });
  }
  if (typeof password !== 'string' || password.length === 0) {
    return NextResponse.json({ error: 'missing password' }, { status: 400 });
  }
  // Constant-time-ish compare.
  const a = Buffer.from(password);
  const b = Buffer.from(expected);
  const ok = a.length === b.length && a.equals(b);
  if (!ok) return NextResponse.json({ error: 'wrong password' }, { status: 401 });

  const session = await getSession();
  session.loggedIn = true;
  await session.save();
  return NextResponse.json({ ok: true });
}
