'use client';
import { useEffect, useState } from 'react';

type Category = { id: string; name: string; sort_order: number };

export default function CategoriesPage() {
  const [cats, setCats] = useState<Category[]>([]);
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function load() {
    const r = await fetch('/api/categories');
    const j = await r.json();
    setCats(j.items ?? []);
  }
  useEffect(() => { load(); }, []);

  async function add() {
    if (!name.trim()) return;
    setBusy(true); setErr(null);
    const r = await fetch('/api/categories', {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ name }),
    });
    const j = await r.json();
    if (!r.ok) setErr(j.error ?? 'Failed');
    else { setName(''); load(); }
    setBusy(false);
  }
  async function rename(id: string, current: string) {
    const next = prompt('Rename category', current);
    if (!next || next === current) return;
    await fetch(`/api/categories/${id}`, {
      method: 'PATCH', headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ name: next }),
    });
    load();
  }
  async function del(id: string) {
    if (!confirm('Delete category? Videos in it will become uncategorized.')) return;
    await fetch(`/api/categories/${id}`, { method: 'DELETE' });
    load();
  }

  return (
    <div className="space-y-4 max-w-md">
      <h1 className="text-lg font-semibold">Categories</h1>
      <div className="flex gap-2">
        <input value={name} onChange={e => setName(e.target.value)} placeholder="New category name"
          className="flex-1 border rounded-lg px-3 py-2 bg-white" />
        <button onClick={add} disabled={busy} className="bg-brand text-white rounded-lg px-3 py-2 text-sm disabled:opacity-60">Add</button>
      </div>
      {err && <div className="text-sm text-red-600">{err}</div>}
      <ul className="bg-white rounded-2xl shadow divide-y">
        {cats.map(c => (
          <li key={c.id} className="flex items-center px-4 py-2 gap-2">
            <span className="flex-1">{c.name}</span>
            <button onClick={() => rename(c.id, c.name)} className="text-sm text-gray-600 hover:text-gray-900">Rename</button>
            <button onClick={() => del(c.id)} className="text-sm text-red-600 hover:underline">Delete</button>
          </li>
        ))}
      </ul>
    </div>
  );
}
