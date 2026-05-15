import { NextResponse } from 'next/server';
import { requireLogin } from '@/lib/auth';
import { parseYouTubeUrl } from '@/lib/url-parser';
import {
  fetchVideos,
  fetchPlaylistMeta,
  fetchPlaylistVideoIds,
  fetchChannelMeta,
} from '@/lib/youtube';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

// Returns a preview card payload for one URL. No DB writes happen here.
export async function POST(req: Request) {
  const auth = await requireLogin();
  if (auth !== true) return auth;

  const { url } = await req.json().catch(() => ({}));
  if (typeof url !== 'string') {
    return NextResponse.json({ error: 'missing url' }, { status: 400 });
  }
  const parsed = parseYouTubeUrl(url);
  if (!parsed) {
    return NextResponse.json({ error: 'unrecognized YouTube URL' }, { status: 400 });
  }

  try {
    if (parsed.kind === 'video') {
      const [v] = await fetchVideos([parsed.id]);
      if (!v) return NextResponse.json({ error: 'video not found' }, { status: 404 });
      return NextResponse.json({ kind: 'video', video: v });
    }

    if (parsed.kind === 'playlist') {
      const meta = await fetchPlaylistMeta(parsed.id);
      if (!meta) return NextResponse.json({ error: 'playlist not found' }, { status: 404 });
      // Cheap count: pull all ids (1 unit per 50 items).
      const ids = await fetchPlaylistVideoIds(parsed.id);
      return NextResponse.json({
        kind: 'playlist',
        playlist: meta,
        itemCount: ids.length,
        thumbnailUrl: `https://i.ytimg.com/vi/${ids[0] ?? ''}/hqdefault.jpg`,
      });
    }

    // channel
    const ch = await fetchChannelMeta(
      parsed.kind === 'channelHandle' ? { handle: parsed.handle } : { channelId: parsed.id }
    );
    if (!ch) return NextResponse.json({ error: 'channel not found' }, { status: 404 });
    const ids = await fetchPlaylistVideoIds(ch.uploadsPlaylistId, 50);
    return NextResponse.json({
      kind: 'channel',
      channel: ch,
      itemCount: ids.length,
      thumbnailUrl: `https://i.ytimg.com/vi/${ids[0] ?? ''}/hqdefault.jpg`,
    });
  } catch (e: any) {
    return NextResponse.json({ error: e.message ?? 'lookup failed' }, { status: 500 });
  }
}
