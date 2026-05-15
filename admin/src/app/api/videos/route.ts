import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
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

// GET /api/videos -- paginated admin list. Returns { items, total }.
export async function GET(req: Request) {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  const { searchParams } = new URL(req.url);
  const page = Math.max(1, parseInt(searchParams.get('page') ?? '1', 10));
  const pageSize = Math.min(100, parseInt(searchParams.get('pageSize') ?? '25', 10));
  const category = searchParams.get('category');
  const search = searchParams.get('q')?.trim();

  let q = db.from('videos').select('*', { count: 'exact' }).order('added_at', { ascending: false });
  if (category && category !== 'all') q = q.eq('category_id', category);
  if (search) q = q.ilike('title', `%${search}%`);

  const from = (page - 1) * pageSize;
  const to = from + pageSize - 1;
  const { data, error, count } = await q.range(from, to);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ items: data ?? [], total: count ?? 0 });
}

// POST /api/videos -- save what the preview showed. Body: { url, categoryId }
export async function POST(req: Request) {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  const { url, categoryId } = await req.json().catch(() => ({}));
  if (typeof url !== 'string' || typeof categoryId !== 'string') {
    return NextResponse.json({ error: 'url and categoryId required' }, { status: 400 });
  }
  const parsed = parseYouTubeUrl(url);
  if (!parsed) return NextResponse.json({ error: 'unrecognized URL' }, { status: 400 });

  try {
    if (parsed.kind === 'video') {
      const [v] = await fetchVideos([parsed.id]);
      if (!v) return NextResponse.json({ error: 'video not found' }, { status: 404 });
      if (!v.embeddable) {
        return NextResponse.json(
          { error: 'This video has embedding disabled by its channel owner and can\'t be played inside the app. Try a different video.' },
          { status: 400 },
        );
      }
      await upsertVideos([v], categoryId, null);
      return NextResponse.json({ ok: true, saved: 1, skipped: 0 });
    }

    if (parsed.kind === 'playlist') {
      const meta = await fetchPlaylistMeta(parsed.id);
      if (!meta) return NextResponse.json({ error: 'playlist not found' }, { status: 404 });
      const source = await upsertSource('playlist', parsed.id, meta.title, categoryId);
      const ids = await fetchPlaylistVideoIds(parsed.id);
      const all = await fetchVideos(ids);
      const playable = all.filter(v => v.embeddable);
      await upsertVideos(playable, categoryId, source.id);
      await db.from('sources').update({ last_synced_at: new Date().toISOString() }).eq('id', source.id);
      return NextResponse.json({ ok: true, saved: playable.length, skipped: all.length - playable.length });
    }

    // channel
    const ch = await fetchChannelMeta(
      parsed.kind === 'channelHandle' ? { handle: parsed.handle } : { channelId: parsed.id }
    );
    if (!ch) return NextResponse.json({ error: 'channel not found' }, { status: 404 });
    const source = await upsertSource('channel', ch.uploadsPlaylistId, ch.title, categoryId);
    const ids = await fetchPlaylistVideoIds(ch.uploadsPlaylistId, 50);
    const all = await fetchVideos(ids);
    const playable = all.filter(v => v.embeddable);
    await upsertVideos(playable, categoryId, source.id);
    await db.from('sources').update({ last_synced_at: new Date().toISOString() }).eq('id', source.id);
    return NextResponse.json({ ok: true, saved: playable.length, skipped: all.length - playable.length });
  } catch (e: any) {
    return NextResponse.json({ error: e.message ?? 'save failed' }, { status: 500 });
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
  vids: { videoId: string; title: string; channelTitle: string; channelId: string; thumbnailUrl: string; durationSeconds: number; publishedAt: string; embeddable: boolean }[],
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
    is_embeddable: v.embeddable,
  }));
  const { error } = await db.from('videos').upsert(rows, { onConflict: 'video_id' });
  if (error) throw new Error(error.message);
}
