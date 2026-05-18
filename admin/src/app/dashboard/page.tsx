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
  const [sort, setSort] = useState('newest');
  const [q, setQ] = useState('');
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [bulkCategoryId, setBulkCategoryId] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  const load = useCallback(async () => {
    setBusy(true);
    const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize), sort });
    if (filter !== 'all') params.set('category', filter);
    if (q.trim()) params.set('q', q.trim());
    const r = await fetch('/api/videos?' + params);
    const j = await r.json();
    setItems(j.items ?? []); setTotal(j.total ?? 0);
    setBusy(false);
  }, [page, pageSize, filter, q, sort]);

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
  function toggleSelected(id: string) {
    setSelected(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }
  function togglePageSelected() {
    setSelected(prev => {
      const next = new Set(prev);
      const allSelected = items.length > 0 && items.every(v => next.has(v.video_id));
      if (allSelected) items.forEach(v => next.delete(v.video_id));
      else items.forEach(v => next.add(v.video_id));
      return next;
    });
  }
  async function bulkMove() {
    const ids = Array.from(selected);
    if (!bulkCategoryId || ids.length === 0) return;
    setBusy(true);
    const r = await fetch('/api/videos/bulk', {
      method: 'PATCH',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ ids, categoryId: bulkCategoryId }),
    });
    const j = await r.json();
    setMsg(r.ok ? `Moved ${j.updated ?? ids.length} videos.` : `Move failed: ${j.error}`);
    setSelected(new Set());
    setBusy(false);
    load();
  }
  async function bulkDelete() {
    const ids = Array.from(selected);
    if (ids.length === 0) return;
    if (!confirm(`Remove ${ids.length} selected videos from the kids app?`)) return;
    setBusy(true);
    const r = await fetch('/api/videos/bulk', {
      method: 'DELETE',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ ids }),
    });
    const j = await r.json();
    setMsg(r.ok ? `Deleted ${j.deleted ?? ids.length} videos.` : `Delete failed: ${j.error}`);
    setSelected(new Set());
    setBusy(false);
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
  const selectedCount = selected.size;
  const pageSelected = items.length > 0 && items.every(v => selected.has(v.video_id));

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2 items-center">
        <select value={filter} onChange={e => { setPage(1); setFilter(e.target.value); }}
          className="border rounded-lg px-3 py-2 text-sm bg-white">
          <option value="all">All categories</option>
          {cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select value={sort} onChange={e => { setPage(1); setSort(e.target.value); }}
          className="border rounded-lg px-3 py-2 text-sm bg-white">
          <option value="newest">Newest first</option>
          <option value="oldest">Oldest first</option>
          <option value="title">Title A-Z</option>
          <option value="category">Category</option>
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
      {selectedCount > 0 && (
        <div className="flex flex-wrap gap-2 items-center bg-white border rounded-lg px-3 py-2 text-sm">
          <span className="font-medium">{selectedCount} selected</span>
          <select value={bulkCategoryId} onChange={e => setBulkCategoryId(e.target.value)}
            className="border rounded-lg px-2 py-1 bg-white">
            <option value="">Move to category...</option>
            {cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <button onClick={bulkMove} disabled={busy || !bulkCategoryId}
            className="border rounded-lg px-3 py-1 bg-white disabled:opacity-50">
            Move
          </button>
          <button onClick={bulkDelete} disabled={busy}
            className="text-red-600 border border-red-200 rounded-lg px-3 py-1 bg-white disabled:opacity-50">
            Delete selected
          </button>
          <button onClick={() => setSelected(new Set())} className="text-gray-600">Clear</button>
        </div>
      )}

      <div className="bg-white rounded-2xl shadow overflow-hidden">
        <div className="hidden md:grid grid-cols-[32px_120px_1fr_180px_140px] gap-3 px-4 py-2 text-xs uppercase tracking-wide text-gray-500 bg-gray-50 border-b">
          <input type="checkbox" checked={pageSelected} onChange={togglePageSelected} aria-label="Select page" />
          <span>Thumbnail</span><span>Title / Channel</span><span>Category</span><span>Actions</span>
        </div>
        {items.length === 0 && !busy && (
          <div className="p-8 text-center text-gray-500 text-sm">No videos yet. Click Add video to start.</div>
        )}
        {items.map(v => (
          <div key={v.video_id}
            className="grid grid-cols-[32px_120px_1fr] md:grid-cols-[32px_120px_1fr_180px_140px] gap-3 px-4 py-3 border-b last:border-b-0 items-center">
            <input
              type="checkbox"
              checked={selected.has(v.video_id)}
              onChange={() => toggleSelected(v.video_id)}
              aria-label={`Select ${v.title}`}
            />
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
