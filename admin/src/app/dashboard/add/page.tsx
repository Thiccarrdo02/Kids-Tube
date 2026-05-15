'use client';
import { useEffect, useState } from 'react';

type Category = { id: string; name: string };

type Preview =
  | { kind: 'video'; video: any }
  | { kind: 'playlist'; playlist: any; itemCount: number; thumbnailUrl: string }
  | { kind: 'channel'; channel: any; itemCount: number; thumbnailUrl: string };

export default function AddPage() {
  const [url, setUrl] = useState('');
  const [cats, setCats] = useState<Category[]>([]);
  const [categoryId, setCategoryId] = useState('');
  const [preview, setPreview] = useState<Preview | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [savedMsg, setSavedMsg] = useState<string | null>(null);

  // Inline "new category" UI state.
  const [addingCat, setAddingCat] = useState(false);
  const [newCatName, setNewCatName] = useState('');

  async function loadCats(autoSelectId?: string) {
    const j = await fetch('/api/categories').then(r => r.json());
    const list: Category[] = j.items ?? [];
    setCats(list);
    if (autoSelectId) {
      setCategoryId(autoSelectId);
    } else if (list.length && !categoryId) {
      setCategoryId(list[0].id);
    }
  }
  useEffect(() => { loadCats(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  async function doPreview() {
    setBusy(true); setErr(null); setPreview(null); setSavedMsg(null);
    try {
      const r = await fetch('/api/preview', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ url }),
      });
      const j = await r.json();
      if (!r.ok) setErr(j.error ?? 'Lookup failed');
      else setPreview(j);
    } finally { setBusy(false); }
  }

  async function save() {
    if (!preview) return;
    setBusy(true); setErr(null);
    try {
      const r = await fetch('/api/videos', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ url, categoryId }),
      });
      const j = await r.json();
      if (!r.ok) { setErr(j.error ?? 'Save failed'); return; }
      const saved = j.saved ?? 0;
      const skipped = j.skipped ?? 0;
      const skipNote = skipped > 0
        ? ` (${skipped} skipped — channel disabled embedding)`
        : '';
      setSavedMsg(`Saved ${saved} video${saved === 1 ? '' : 's'}.${skipNote}`);
      setPreview(null); setUrl('');
    } finally { setBusy(false); }
  }

  async function createCategory() {
    const name = newCatName.trim();
    if (!name) return;
    setBusy(true); setErr(null);
    try {
      const r = await fetch('/api/categories', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ name }),
      });
      const j = await r.json();
      if (!r.ok) { setErr(j.error ?? 'Add failed'); return; }
      setNewCatName(''); setAddingCat(false);
      await loadCats(j.id);
    } finally { setBusy(false); }
  }

  return (
    <div className="space-y-4 max-w-xl">
      <h1 className="text-lg font-semibold">Add content</h1>
      <p className="text-sm text-gray-600">
        Paste a YouTube video, playlist, or channel URL. The whole playlist or
        the latest 50 channel uploads will be added.
      </p>

      <div className="flex gap-2">
        <input value={url} onChange={e => setUrl(e.target.value)}
          placeholder="https://www.youtube.com/..."
          className="flex-1 border rounded-lg px-3 py-2 bg-white" />
        <button onClick={doPreview} disabled={busy || !url}
          className="bg-gray-900 text-white rounded-lg px-3 py-2 text-sm disabled:opacity-60">
          {busy ? 'Looking up...' : 'Look up'}
        </button>
      </div>

      {err && <div className="text-sm text-red-600">{err}</div>}
      {savedMsg && <div className="text-sm text-green-700 bg-green-50 border border-green-200 rounded p-2">{savedMsg}</div>}

      {preview && (
        <div className="bg-white border rounded-2xl p-4 space-y-3">
          {preview.kind === 'video' && (
            <div className="flex gap-3">
              <img src={preview.video.thumbnailUrl} className="w-40 aspect-video object-cover rounded-md" alt="" />
              <div>
                <div className="font-medium">{preview.video.title}</div>
                <div className="text-sm text-gray-600">{preview.video.channelTitle}</div>
                <div className="text-xs text-gray-500">Single video</div>
              </div>
            </div>
          )}
          {preview.kind === 'playlist' && (
            <div className="flex gap-3">
              <img src={preview.thumbnailUrl} className="w-40 aspect-video object-cover rounded-md" alt="" />
              <div>
                <div className="font-medium">{preview.playlist.title}</div>
                <div className="text-sm text-gray-600">{preview.playlist.channelTitle}</div>
                <div className="text-xs text-gray-500">Playlist · {preview.itemCount} videos</div>
              </div>
            </div>
          )}
          {preview.kind === 'channel' && (
            <div className="flex gap-3">
              <img src={preview.thumbnailUrl} className="w-40 aspect-video object-cover rounded-md" alt="" />
              <div>
                <div className="font-medium">{preview.channel.title}</div>
                <div className="text-xs text-gray-500">Channel · latest {preview.itemCount} uploads</div>
              </div>
            </div>
          )}

          <div className="pt-2 border-t space-y-2">
            <div className="flex items-center gap-2 flex-wrap">
              <label className="text-sm text-gray-600">Category</label>
              <select value={categoryId} onChange={e => setCategoryId(e.target.value)}
                className="border rounded-lg px-2 py-1 text-sm bg-white">
                {cats.length === 0 && <option value="">(none — add one)</option>}
                {cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
              {!addingCat && (
                <button onClick={() => setAddingCat(true)}
                  className="text-sm text-gray-600 hover:text-gray-900 underline">
                  + New
                </button>
              )}
              <button onClick={save} disabled={busy || !categoryId}
                className="ml-auto bg-brand text-white rounded-lg px-3 py-2 text-sm disabled:opacity-60">
                {busy ? 'Saving...' : 'Confirm'}
              </button>
            </div>
            {addingCat && (
              <div className="flex gap-2 items-center">
                <input autoFocus value={newCatName}
                  onChange={e => setNewCatName(e.target.value)}
                  onKeyDown={e => { if (e.key === 'Enter') createCategory(); }}
                  placeholder="New category name (e.g. Nasheeds)"
                  className="flex-1 border rounded-lg px-2 py-1 text-sm bg-white" />
                <button onClick={createCategory} disabled={busy || !newCatName.trim()}
                  className="text-sm bg-gray-900 text-white rounded-lg px-3 py-1 disabled:opacity-60">
                  Add
                </button>
                <button onClick={() => { setAddingCat(false); setNewCatName(''); }}
                  className="text-sm text-gray-600">Cancel</button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
