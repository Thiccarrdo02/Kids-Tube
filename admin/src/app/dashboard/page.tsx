'use client';
import { useEffect, useState, useCallback } from 'react';

type Video = {
  video_id: string; title: string; channel_title: string | null;
  thumbnail_url: string | null; duration_seconds: number;
  category_id: string | null; added_at: string;
};
type Category = { id: string; name: string };

function fmtDuration(s: number) {
  if (!s) return '';
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60;
  return h > 0 ? `${h}:${String(m).padStart(2,'0')}:${String(sec).padStart(2,'0')}` : `${m}:${String(sec).padStart(2,'0')}`;
}

export default function VideosPage() {
  const [items, setItems] = useState<Video[]>([]);
  const [total, setTotal] = useState(0);
  const [cats, setCats] = useState<Category[]>([]);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(25);
  const [filter, setFilter] = useState<string>('all');
  const [q, setQ] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  const load = useCallback(async () => {
    setBusy(true);
    const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) });
    if (filter !== 'all') params.set('category', filter);
    if (q.trim()) params.set('q', q.trim());
    const r = await fetch('/api/videos?' + params);
    const j = await r.json();
    setItems(j.items ?? []); setTotal(j.total ?? 0);
    setBusy(false);
  }, [page, pageSize, filter, q]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    fetch('/api/categories').then(r => r.json()).then(j => setCats(j.items ?? []));
  }, []);

  async function changeCategory(id: string, categoryId: string) {
    await fetch(`/api/videos/${id}`, {
      method: 'PATCH',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ categoryId }),
    });
    load();
  }
  async function del(id: string) {
    if (!confirm('Remove this video from the kids app?')) return;
    await fetch(`/api/videos/${id}`, { method: 'DELETE' });
    load();
  }
  async function refreshSources() {
    setBusy(true); setMsg(null);
    const r = await fetch('/api/refresh', { method: 'POST' });
    const j = await r.json();
    setMsg(r.ok ? `Refresh done. ${j.totalAdded} new videos.` : `Refresh failed: ${j.error}`);
    setBusy(false);
    load();
  }

  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2 items-center">
        <select value={filter} onChange={e => { setPage(1); setFilter(e.target.value); }}
          className="border rounded-lg px-3 py-2 text-sm bg-white">
          <option value="all">All categories</option>
          {cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input value={q} onChange={e => { setPage(1); setQ(e.target.value); }}
          placeholder="Search title..." className="border rounded-lg px-3 py-2 text-sm bg-white flex-1 min-w-[160px]" />
        <button onClick={refreshSources} disabled={busy}
          className="text-sm bg-white border rounded-lg px-3 py-2 hover:bg-gray-50 disabled:opacity-60">
          Refresh sources
        </button>
        <a href="/dashboard/add" className="text-sm bg-brand text-white rounded-lg px-3 py-2">+ Add video</a>
      </div>

      {msg && <div className="text-sm text-gray-700">{msg}</div>}

      <div className="bg-white rounded-2xl shadow overflow-hidden">
        <div className="hidden md:grid grid-cols-[120px_1fr_180px_140px] gap-3 px-4 py-2 text-xs uppercase tracking-wide text-gray-500 bg-gray-50 border-b">
          <span>Thumbnail</span><span>Title / Channel</span><span>Category</span><span>Actions</span>
        </div>
        {items.length === 0 && !busy && (
          <div className="p-8 text-center text-gray-500 text-sm">No videos yet. Click "Add video" to start.</div>
        )}
        {items.map(v => (
          <div key={v.video_id}
            className="grid grid-cols-[120px_1fr] md:grid-cols-[120px_1fr_180px_140px] gap-3 px-4 py-3 border-b last:border-b-0 items-center">
            <div className="relative">
              {v.thumbnail_url
                ? <img src={v.thumbnail_url} alt="" className="w-[120px] aspect-video object-cover rounded-md bg-gray-200" />
                : <div className="w-[120px] aspect-video bg-gray-200 rounded-md" />}
              {v.duration_seconds > 0 && (
                <span className="absolute bottom-1 right-1 bg-black/80 text-white text-[10px] px-1 rounded">{fmtDuration(v.duration_seconds)}</span>
              )}
            </div>
            <div className="min-w-0">
              <div className="font-medium line-clamp-2">{v.title}</div>
              <div className="text-xs text-gray-500">{v.channel_title}</div>
              <div className="text-xs text-gray-400">{new Date(v.added_at).toLocaleDateString()}</div>
            </div>
            <select value={v.category_id ?? ''} onChange={e => changeCategory(v.video_id, e.target.value)}
              className="border rounded-lg px-2 py-1 text-sm bg-white">
              <option value="">(none)</option>
              {cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <button onClick={() => del(v.video_id)} className="text-sm text-red-600 hover:underline justify-self-start">Delete</button>
          </div>
        ))}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center gap-2 justify-center text-sm">
          <button disabled={page <= 1} onClick={() => setPage(p => p - 1)} className="border rounded px-3 py-1 disabled:opacity-40 bg-white">Prev</button>
          <span>Page {page} of {totalPages} ({total} total)</span>
          <button disabled={page >= totalPages} onClick={() => setPage(p => p + 1)} className="border rounded px-3 py-1 disabled:opacity-40 bg-white">Next</button>
        </div>
      )}
    </div>
  );
}
