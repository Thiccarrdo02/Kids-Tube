'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';

export default function LoginPage() {
  const router = useRouter();
  const [pw, setPw] = useState('');
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true); setErr(null);
    try {
      const r = await fetch('/api/login', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ password: pw }),
      });
      if (!r.ok) {
        const j = await r.json().catch(() => ({}));
        setErr(j.error ?? 'Login failed');
      } else {
        router.push('/dashboard');
        router.refresh();
      }
    } finally { setBusy(false); }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <form onSubmit={submit} className="w-full max-w-sm bg-white rounded-2xl shadow p-6 space-y-4">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-md bg-brand grid place-items-center">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="white"><path d="M8 5v14l11-7z"/></svg>
          </div>
          <h1 className="text-xl font-semibold">KidsTube Admin</h1>
        </div>
        <input
          type="password"
          autoFocus
          placeholder="Admin password"
          value={pw}
          onChange={e => setPw(e.target.value)}
          className="w-full border rounded-lg px-3 py-2 outline-none focus:border-brand"
        />
        {err && <p className="text-sm text-red-600">{err}</p>}
        <button disabled={busy} className="w-full bg-brand text-white rounded-lg py-2 font-medium disabled:opacity-60">
          {busy ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </div>
  );
}
