'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';

type SetupStatus = {
  ready: boolean;
  autoSetupAvailable: boolean;
  schemaSql: string;
};

export default function SetupPage() {
  const router = useRouter();
  const [status, setStatus] = useState<SetupStatus | null>(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    fetch('/api/setup').then(r => r.json()).then(setStatus);
  }, []);

  async function runAuto() {
    setBusy(true); setErr(null);
    try {
      const r = await fetch('/api/setup', { method: 'POST' });
      const j = await r.json();
      if (!r.ok) { setErr(j.error ?? 'Setup failed'); return; }
      // success -- go to dashboard
      router.push('/dashboard');
      router.refresh();
    } finally { setBusy(false); }
  }

  async function recheck() {
    setBusy(true);
    try {
      const j = await fetch('/api/setup').then(r => r.json());
      setStatus(j);
      if (j.ready) { router.push('/dashboard'); router.refresh(); }
    } finally { setBusy(false); }
  }

  if (!status) return <p className="text-sm text-gray-500">Checking database…</p>;

  if (status.ready) {
    return (
      <div className="space-y-3 max-w-xl">
        <h1 className="text-lg font-semibold">Database is ready</h1>
        <a href="/dashboard" className="inline-block bg-brand text-white rounded-lg px-3 py-2 text-sm">
          Go to dashboard
        </a>
      </div>
    );
  }

  return (
    <div className="space-y-5 max-w-2xl">
      <h1 className="text-lg font-semibold">One-time database setup</h1>
      <p className="text-sm text-gray-600">
        Your Supabase project doesn&apos;t have the KidsTube tables yet. Pick one of the two paths below.
      </p>

      <section className="bg-white border rounded-2xl p-4 space-y-3">
        <h2 className="font-semibold">Option A — One click (recommended)</h2>
        <p className="text-sm text-gray-600">
          This requires the <code className="bg-gray-100 px-1 rounded">SUPABASE_DB_URL</code> env var in Vercel.
        </p>
        {status.autoSetupAvailable ? (
          <button onClick={runAuto} disabled={busy}
            className="bg-brand text-white rounded-lg px-3 py-2 text-sm disabled:opacity-60">
            {busy ? 'Setting up…' : 'Initialize database'}
          </button>
        ) : (
          <div className="text-sm text-amber-800 bg-amber-50 border border-amber-200 rounded p-3">
            <p className="mb-2"><strong>SUPABASE_DB_URL is not set in Vercel.</strong> Add it once, then redeploy:</p>
            <ol className="list-decimal pl-5 space-y-1">
              <li>Supabase dashboard → your project → <strong>Settings → Database</strong></li>
              <li>Under <strong>Connection string</strong>, click <strong>URI</strong> and pick <strong>Transaction</strong> mode</li>
              <li>Copy the full <code>postgresql://…</code> URL (it includes your DB password)</li>
              <li>In Vercel: <strong>Settings → Environment Variables</strong>, add <code>SUPABASE_DB_URL</code> with that value</li>
              <li><strong>Redeploy</strong> (Deployments tab → top deployment → ⋯ → Redeploy)</li>
              <li>Come back here and click <strong>Recheck</strong></li>
            </ol>
            <button onClick={recheck} disabled={busy}
              className="mt-3 bg-gray-900 text-white rounded-lg px-3 py-1 text-sm">
              {busy ? 'Checking…' : 'Recheck'}
            </button>
          </div>
        )}
        {err && <p className="text-sm text-red-600">{err}</p>}
      </section>

      <section className="bg-white border rounded-2xl p-4 space-y-3">
        <h2 className="font-semibold">Option B — Run the SQL manually</h2>
        <ol className="text-sm text-gray-700 list-decimal pl-5 space-y-1">
          <li>Open Supabase → your project → <strong>SQL Editor</strong> → <strong>+ New query</strong></li>
          <li>Paste the SQL below and click <strong>Run</strong></li>
          <li>Click <strong>Recheck</strong>.</li>
        </ol>
        <div className="flex items-center justify-end gap-2">
          <button
            onClick={async () => {
              await navigator.clipboard.writeText(status.schemaSql);
              setCopied(true);
              setTimeout(() => setCopied(false), 1500);
            }}
            className="text-sm bg-gray-900 text-white rounded-lg px-3 py-1"
          >
            {copied ? 'Copied!' : 'Copy SQL'}
          </button>
          <button onClick={recheck} disabled={busy} className="text-sm border rounded-lg px-3 py-1 bg-white">
            {busy ? 'Checking…' : 'Recheck'}
          </button>
        </div>
        <pre className="bg-gray-900 text-gray-100 text-xs p-3 rounded overflow-auto max-h-72 whitespace-pre">
{status.schemaSql.trim()}
        </pre>
      </section>
    </div>
  );
}
