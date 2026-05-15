'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';

type Category = { id: string; name: string };

type Preview =
  | { kind: 'video'; video: any }
  | { kind: 'playlist'; playlist: any; itemCount: number; thumbnailUrl: string }
  | { kind: 'channel'; channel: any; itemCount: number; thumbnailUrl: string };

export default function AddPage() {
  const router = useRouter();
  const [url, setUrl] = useState('');
  const [cats, setCats] = useState<Category[]>([]);
  const [categoryId, setCategoryId] = useState('');
  const [preview, setPreview] = useState<Preview | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [savedMsg, setSavedMsg] = useState<string | null>(null);

  useEffect(() => {
    fetch('/api/categories').then(r => r.json()).then(j => {
      setCats(j.items ?? []);
      if (j.items?.length && !categoryId) setCategoryId(j.items[0].id);
    });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

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
      setSavedMsg(`Saved ${j.saved} video${j.saved === 1 ? '' : 's'}.`);
      setPreview(null); setUrl('');
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

          <div className="flex items-center gap-2 pt-2 border-t">
            <label className="text-sm text-gray-600">Category</label>
            <select value={categoryId} onChange={e => setCategoryId(e.target.value)}
              className="border rounded-lg px-2 py-1 text-sm bg-white">
              {cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <button onClick={save} disabled={busy || !categoryId}
              className="ml-auto bg-brand text-white rounded-lg px-3 py-2 text-sm disabled:opacity-60">
              {busy ? 'Saving...' : 'Confirm'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
