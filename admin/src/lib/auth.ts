import { getIronSession, SessionOptions } from 'iron-session';
import { cookies } from 'next/headers';

export type Session = { loggedIn?: boolean };

export const sessionOptions: SessionOptions = {
  cookieName: 'kidstube_admin',
  password: process.env.SESSION_SECRET ?? 'dev-only-secret-please-change-32chars',
  cookieOptions: {
    secure: process.env.NODE_ENV === 'production',
    httpOnly: true,
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 30, // 30 days
  },
};

export async function getSession() {
  return getIronSession<Session>(cookies(), sessionOptions);
}

export async function requireLogin(): Promise<true | Response> {
  const s = await getSession();
  if (!s.loggedIn) {
    return new Response(JSON.stringify({ error: 'unauthorized' }), {
      status: 401,
      headers: { 'content-type': 'application/json' },
    });
  }
  return true;
}
