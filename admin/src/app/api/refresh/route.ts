import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
import { db } from '@/lib/db';
import { fetchPlaylistVideoIds, fetchVideos } from '@/lib/youtube';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';
export const maxDuration = 60;

// Re-syncs every saved source. For channels, source_id is the uploads playlist
// id, so we treat all sources uniformly with playlistItems.list.
export async function POST() {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  const { data: sources, error } = await db.from('sources').select('*');
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });

  let totalAdded = 0;
  const perSource: { source_id: string; title: string | null; new: number }[] = [];

  for (const src of sources ?? []) {
    const cap = src.kind === 'channel' ? 50 : Infinity;
    const ids = await fetchPlaylistVideoIds(src.source_id, cap);

    // Figure out which ids we don't already have, to avoid refetching all
    // metadata. (videos.list is 1 unit per 50 ids regardless.)
    const { data: existing } = await db
      .from('videos')
      .select('video_id')
      .in('video_id', ids);
    const have = new Set((existing ?? []).map(r => r.video_id));
    const fresh = ids.filter(id => !have.has(id));

    if (fresh.length > 0) {
      const vids = await fetchVideos(fresh);
      const rows = vids.map(v => ({
        video_id: v.videoId,
        title: v.title,
        channel_title: v.channelTitle,
        channel_id: v.channelId,
        thumbnail_url: v.thumbnailUrl,
        duration_seconds: v.durationSeconds,
        published_at: v.publishedAt,
        category_id: src.category_id,
        source_id: src.id,
      }));
      const { error: insErr } = await db.from('videos').upsert(rows, { onConflict: 'video_id' });
      if (insErr) return NextResponse.json({ error: insErr.message }, { status: 500 });
      totalAdded += rows.length;
    }
    await db.from('sources').update({ last_synced_at: new Date().toISOString() }).eq('id', src.id);
    perSource.push({ source_id: src.source_id, title: src.title, new: fresh.length });
  }

  return NextResponse.json({ ok: true, totalAdded, perSource });
}
