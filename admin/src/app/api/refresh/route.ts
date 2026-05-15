import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
import { db } from '@/lib/db';
import { fetchPlaylistVideoIds, fetchVideos } from '@/lib/youtube';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';
export const maxDuration = 60;

// Refreshes the feed in two passes:
//   1. For each saved source (playlist/channel), pull the latest item list
//      and insert any new ones.
//   2. Recheck *every* existing video's embeddable flag. YouTube channels
//      sometimes toggle "Allow embedding" off later; this pass catches that
//      so non-embeddable videos disappear from the kids feed.
export async function POST() {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  const { data: sources, error } = await db.from('sources').select('*');
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });

  let totalAdded = 0;
  let totalSkipped = 0;
  let totalRechecked = 0;
  let totalMarkedUnplayable = 0;
  const perSource: { source_id: string; title: string | null; added: number; skipped: number }[] = [];

  // -- Pass 1: pull new items per source --
  for (const src of sources ?? []) {
    const cap = src.kind === 'channel' ? 50 : Infinity;
    const ids = await fetchPlaylistVideoIds(src.source_id, cap);
    const { data: existing } = await db
      .from('videos')
      .select('video_id')
      .in('video_id', ids);
    const have = new Set((existing ?? []).map(r => r.video_id));
    const freshIds = ids.filter(id => !have.has(id));

    let added = 0;
    let skipped = 0;
    if (freshIds.length > 0) {
      const vids = await fetchVideos(freshIds);
      const playable = vids.filter(v => v.embeddable);
      skipped = vids.length - playable.length;
      const rows = playable.map(v => ({
        video_id: v.videoId,
        title: v.title,
        channel_title: v.channelTitle,
        channel_id: v.channelId,
        thumbnail_url: v.thumbnailUrl,
        duration_seconds: v.durationSeconds,
        published_at: v.publishedAt,
        category_id: src.category_id,
        source_id: src.id,
        is_embeddable: true,
      }));
      if (rows.length > 0) {
        const { error: insErr } = await db.from('videos').upsert(rows, { onConflict: 'video_id' });
        if (insErr) return NextResponse.json({ error: insErr.message }, { status: 500 });
      }
      added = rows.length;
      totalAdded += added;
      totalSkipped += skipped;
    }
    await db.from('sources').update({ last_synced_at: new Date().toISOString() }).eq('id', src.id);
    perSource.push({ source_id: src.source_id, title: src.title, added, skipped });
  }

  // -- Pass 2: recheck embeddable for every existing video --
  const { data: allVideos } = await db.from('videos').select('video_id');
  const allIds = (allVideos ?? []).map(v => v.video_id);
  if (allIds.length > 0) {
    const fresh = await fetchVideos(allIds);
    const freshById = new Map(fresh.map(v => [v.videoId, v]));
    // Videos that disappeared from YouTube entirely (deleted/private) -- mark unplayable.
    const missing = allIds.filter(id => !freshById.has(id));
    for (const id of missing) {
      await db.from('videos').update({ is_embeddable: false }).eq('video_id', id);
      totalMarkedUnplayable++;
    }
    for (const [id, v] of freshById) {
      await db.from('videos').update({ is_embeddable: v.embeddable }).eq('video_id', id);
      if (!v.embeddable) totalMarkedUnplayable++;
      totalRechecked++;
    }
  }

  return NextResponse.json({
    ok: true,
    totalAdded,
    totalSkipped,
    totalRechecked,
    totalMarkedUnplayable,
    perSource,
  });
}
