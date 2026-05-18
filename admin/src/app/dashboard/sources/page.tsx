'use client';
import { useEffect, useState } from 'react';

type Source = {
  id: string;
  kind: 'playlist' | 'channel';
  source_id: string;
  title: string | null;
  category_id: string | null;
  last_synced_at: string | null;
  video_count: number;
};
type Category = { id: string; name: string };

export default function SourcesPage() {
  const [sources, setSources] = useState<Source[]>([]);
  const [cats, setCats] = useState<Category[]>([]);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  async function load() {
    const [s, c] = await Promise.all([
      fetch('/api/sources').then(r => r.json()),
      fetch('/api/categories').then(r => r.json()),
    ]);
    setSources(s.items ?? []);
    setCats(c.items ?? []);
  }
  useEffect(() => { load(); }, []);

  async function refresh() {
    setBusy(true); setMsg(null);
    const r = await fetch('/api/refresh', { method: 'POST' });
    const j = await r.json();
    setMsg(r.ok ? `Refresh done. ${j.totalAdded} new videos, ${j.totalSkipped} skipped.` : `Refresh failed: ${j.error}`);
    setBusy(false);
    load();
  }

  async function remove(id: string) {
    if (!confirm('Stop refreshing this source? Existing videos will stay in the app.')) return;
    setBusy(true);
    const r = await fetch(`/api/sources/${id}`, { method: 'DELETE' });
    const j = await r.json().catch(() => ({}));
    setMsg(r.ok ? 'Source removed.' : `Delete failed: ${j.error}`);
    setBusy(false);
    load();
  }

  function categoryName(id: string | null) {
    return cats.find(c => c.id === id)?.name ?? 'Uncategorized';
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <h1 className="text-lg font-semibold">Sources</h1>
        <button onClick={refresh} disabled={busy}
          className="ml-auto text-sm bg-white border rounded-lg px-3 py-2 hover:bg-gray-50 disabled:opacity-60">
          Refresh all
        </button>
      </div>
      {msg && <div className="text-sm text-gray-700">{msg}</div>}
      <div className="bg-white rounded-2xl shadow overflow-hidden">
        <div className="hidden md:grid grid-cols-[110px_1fr_150px_120px_170px_90px] gap-3 px-4 py-2 text-xs uppercase tracking-wide text-gray-500 bg-gray-50 border-b">
          <span>Type</span><span>Title / ID</span><span>Category</span><span>Videos</span><span>Last synced</span><span>Action</span>
        </div>
        {sources.length === 0 && (
          <div className="p-8 text-center text-gray-500 text-sm">No playlist or channel sources yet.</div>
        )}
        {sources.map(source => (
          <div key={source.id}
            className="grid md:grid-cols-[110px_1fr_150px_120px_170px_90px] gap-3 px-4 py-3 border-b last:border-b-0 items-center text-sm">
            <span className="capitalize">{source.kind}</span>
            <div className="min-w-0">
              <div className="font-medium truncate">{source.title ?? '(untitled source)'}</div>
              <div className="text-xs text-gray-500 truncate">{source.source_id}</div>
            </div>
            <span>{categoryName(source.category_id)}</span>
            <span>{source.video_count}</span>
            <span>{source.last_synced_at ? new Date(source.last_synced_at).toLocaleString() : 'Never'}</span>
            <button onClick={() => remove(source.id)} className="text-red-600 hover:underline justify-self-start">
              Remove
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
