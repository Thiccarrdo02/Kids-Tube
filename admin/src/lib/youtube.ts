// Thin wrapper around YouTube Data API v3.
// Only uses playlistItems.list, videos.list, channels.list (1 unit each).
// search.list (100 units) is deliberately NOT used.

const API = 'https://www.googleapis.com/youtube/v3';

function key(): string {
  const k = process.env.YOUTUBE_API_KEY;
  if (!k) throw new Error('YOUTUBE_API_KEY not set');
  return k;
}

async function yt<T = any>(path: string, params: Record<string, string>): Promise<T> {
  const qs = new URLSearchParams({ ...params, key: key() }).toString();
  const r = await fetch(`${API}/${path}?${qs}`);
  if (!r.ok) {
    const body = await r.text();
    throw new Error(`YouTube API ${r.status}: ${body.slice(0, 300)}`);
  }
  return r.json();
}

export type VideoMeta = {
  videoId: string;
  title: string;
  channelTitle: string;
  channelId: string;
  thumbnailUrl: string;
  durationSeconds: number;
  publishedAt: string;
};

// ISO 8601 duration -> seconds (e.g. PT1H2M3S, PT45S)
export function parseDuration(iso: string): number {
  const m = iso.match(/^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?$/);
  if (!m) return 0;
  return (+m[1] || 0) * 3600 + (+m[2] || 0) * 60 + (+m[3] || 0);
}

function pickThumb(thumbs: any): string {
  return (
    thumbs?.maxres?.url ||
    thumbs?.standard?.url ||
    thumbs?.high?.url ||
    thumbs?.medium?.url ||
    thumbs?.default?.url ||
    ''
  );
}

export async function fetchVideos(ids: string[]): Promise<VideoMeta[]> {
  if (ids.length === 0) return [];
  const out: VideoMeta[] = [];
  for (let i = 0; i < ids.length; i += 50) {
    const chunk = ids.slice(i, i + 50);
    const data = await yt<any>('videos', {
      part: 'snippet,contentDetails',
      id: chunk.join(','),
      maxResults: '50',
    });
    for (const item of data.items ?? []) {
      out.push({
        videoId: item.id,
        title: item.snippet.title,
        channelTitle: item.snippet.channelTitle,
        channelId: item.snippet.channelId,
        thumbnailUrl: pickThumb(item.snippet.thumbnails),
        durationSeconds: parseDuration(item.contentDetails.duration),
        publishedAt: item.snippet.publishedAt,
      });
    }
  }
  return out;
}

export type PlaylistMeta = { id: string; title: string; channelTitle: string };

export async function fetchPlaylistMeta(playlistId: string): Promise<PlaylistMeta | null> {
  const data = await yt<any>('playlists', {
    part: 'snippet',
    id: playlistId,
    maxResults: '1',
  });
  const item = data.items?.[0];
  if (!item) return null;
  return {
    id: item.id,
    title: item.snippet.title,
    channelTitle: item.snippet.channelTitle,
  };
}

// Returns all video ids in a playlist. cap = stop after this many (default unlimited).
export async function fetchPlaylistVideoIds(
  playlistId: string,
  cap = Infinity
): Promise<string[]> {
  const ids: string[] = [];
  let pageToken: string | undefined;
  do {
    const data = await yt<any>('playlistItems', {
      part: 'contentDetails',
      playlistId,
      maxResults: '50',
      ...(pageToken ? { pageToken } : {}),
    });
    for (const it of data.items ?? []) {
      const id = it.contentDetails?.videoId;
      if (id) ids.push(id);
      if (ids.length >= cap) return ids;
    }
    pageToken = data.nextPageToken;
  } while (pageToken);
  return ids;
}

// Channel resolution: handle or channelId -> uploads playlist + channel title.
export type ChannelMeta = {
  channelId: string;
  title: string;
  uploadsPlaylistId: string;
};

export async function fetchChannelMeta(opts: {
  channelId?: string;
  handle?: string;
}): Promise<ChannelMeta | null> {
  const params: Record<string, string> = { part: 'snippet,contentDetails', maxResults: '1' };
  if (opts.channelId) params.id = opts.channelId;
  else if (opts.handle) params.forHandle = opts.handle.startsWith('@') ? opts.handle : '@' + opts.handle;
  else return null;
  const data = await yt<any>('channels', params);
  const item = data.items?.[0];
  if (!item) return null;
  return {
    channelId: item.id,
    title: item.snippet.title,
    uploadsPlaylistId: item.contentDetails.relatedPlaylists.uploads,
  };
}
