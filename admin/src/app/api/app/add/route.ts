// Stateless add-video endpoint used by the Android app's parental
// settings. Accepts the admin password in the body so the phone never
// has to juggle session cookies.
//
// Body: { password, url, categoryId?, categoryName? }
//
// If categoryName is given and doesn't exist yet, we create it.
// Returns { ok: true, saved: <count> } or { error: ... }.

import { NextResponse } from 'next/server';
import { db } from '@/lib/db';
import { parseYouTubeUrl } from '@/lib/url-parser';
import {
  fetchVideos,
  fetchPlaylistMeta,
  fetchPlaylistVideoIds,
  fetchChannelMeta,
} from '@/lib/youtube';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

function corsHeaders() {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
  };
}
function jsonCors(body: any, status = 200) {
  return NextResponse.json(body, { status, headers: corsHeaders() });
}

export async function OPTIONS() {
  return new NextResponse(null, { status: 204, headers: corsHeaders() });
}

export async function POST(req: Request) {
  const { password, url, categoryId, categoryName } = await req.json().catch(() => ({}));

  const expected = process.env.ADMIN_PASSWORD;
  if (!expected) return jsonCors({ error: 'admin not configured' }, 500);
  if (typeof password !== 'string' || password !== expected) {
    return jsonCors({ error: 'wrong password' }, 401);
  }
  if (typeof url !== 'string') {
    return jsonCors({ error: 'url required' }, 400);
  }

  // Resolve / create the target category.
  let catId: string | null = typeof categoryId === 'string' && categoryId ? categoryId : null;
  if (!catId && typeof categoryName === 'string' && categoryName.trim()) {
    const name = categoryName.trim();
    // Try fetching first (idempotent for repeated calls).
    const { data: existing } = await db
      .from('categories').select('id').eq('name', name).maybeSingle();
    if (existing) {
      catId = existing.id;
    } else {
      const { data: inserted, error } = await db
        .from('categories').insert({ name, sort_order: 50 }).select('id').single();
      if (error) return jsonCors({ error: error.message }, 500);
      catId = inserted.id;
    }
  }
  if (!catId) return jsonCors({ error: 'category required' }, 400);

  const parsed = parseYouTubeUrl(url);
  if (!parsed) return jsonCors({ error: 'unrecognized URL' }, 400);

  try {
    if (parsed.kind === 'video') {
      const [v] = await fetchVideos([parsed.id]);
      if (!v) return jsonCors({ error: 'video not found' }, 404);
      await upsertVideos([v], catId, null);
      return jsonCors({ ok: true, saved: 1 });
    }
    if (parsed.kind === 'playlist') {
      const meta = await fetchPlaylistMeta(parsed.id);
      if (!meta) return jsonCors({ error: 'playlist not found' }, 404);
      const src = await upsertSource('playlist', parsed.id, meta.title, catId);
      const ids = await fetchPlaylistVideoIds(parsed.id);
      const vids = await fetchVideos(ids);
      await upsertVideos(vids, catId, src.id);
      await db.from('sources').update({ last_synced_at: new Date().toISOString() }).eq('id', src.id);
      return jsonCors({ ok: true, saved: vids.length });
    }
    const ch = await fetchChannelMeta(
      parsed.kind === 'channelHandle' ? { handle: parsed.handle } : { channelId: parsed.id }
    );
    if (!ch) return jsonCors({ error: 'channel not found' }, 404);
    const src = await upsertSource('channel', ch.uploadsPlaylistId, ch.title, catId);
    const ids = await fetchPlaylistVideoIds(ch.uploadsPlaylistId, 50);
    const vids = await fetchVideos(ids);
    await upsertVideos(vids, catId, src.id);
    await db.from('sources').update({ last_synced_at: new Date().toISOString() }).eq('id', src.id);
    return jsonCors({ ok: true, saved: vids.length });
  } catch (e: any) {
    return jsonCors({ error: e.message ?? 'add failed' }, 500);
  }
}

async function upsertSource(kind: 'playlist' | 'channel', sourceId: string, title: string, categoryId: string) {
  const { data, error } = await db
    .from('sources')
    .upsert({ kind, source_id: sourceId, title, category_id: categoryId }, { onConflict: 'source_id' })
    .select()
    .single();
  if (error) throw new Error(error.message);
  return data;
}

async function upsertVideos(
  vids: { videoId: string; title: string; channelTitle: string; channelId: string; thumbnailUrl: string; durationSeconds: number; publishedAt: string }[],
  categoryId: string,
  sourceId: string | null,
) {
  if (vids.length === 0) return;
  const rows = vids.map(v => ({
    video_id: v.videoId,
    title: v.title,
    channel_title: v.channelTitle,
    channel_id: v.channelId,
    thumbnail_url: v.thumbnailUrl,
    duration_seconds: v.durationSeconds,
    published_at: v.publishedAt,
    category_id: categoryId,
    source_id: sourceId,
  }));
  const { error } = await db.from('videos').upsert(rows, { onConflict: 'video_id' });
  if (error) throw new Error(error.message);
}
