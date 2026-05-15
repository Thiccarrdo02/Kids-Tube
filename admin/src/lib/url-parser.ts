// Parses any YouTube URL the parent might paste into one of three shapes.

export type ParsedUrl =
  | { kind: 'video'; id: string }
  | { kind: 'playlist'; id: string }
  | { kind: 'channelHandle'; handle: string }
  | { kind: 'channelId'; id: string };

export function parseYouTubeUrl(input: string): ParsedUrl | null {
  let raw = input.trim();
  if (!raw) return null;
  if (!/^https?:\/\//i.test(raw)) raw = 'https://' + raw;
  let u: URL;
  try { u = new URL(raw); } catch { return null; }

  const host = u.hostname.replace(/^www\./, '').toLowerCase();
  const isYT =
    host === 'youtube.com' || host === 'm.youtube.com' || host === 'youtu.be';
  if (!isYT) return null;

  // Playlist URL takes priority -- youtube.com/playlist?list=... or a /watch URL
  // with both v= and list= (we treat list= as the user's intent when they paste
  // a /playlist URL specifically).
  if (u.pathname === '/playlist') {
    const list = u.searchParams.get('list');
    if (list) return { kind: 'playlist', id: list };
  }

  // youtu.be/<id>
  if (host === 'youtu.be') {
    const id = u.pathname.slice(1).split('/')[0];
    if (id) return { kind: 'video', id };
  }

  // /watch?v=...
  if (u.pathname === '/watch') {
    const v = u.searchParams.get('v');
    if (v) return { kind: 'video', id: v };
  }

  // /shorts/<id>
  const shorts = u.pathname.match(/^\/shorts\/([\w-]{6,})/);
  if (shorts) return { kind: 'video', id: shorts[1] };

  // /channel/UCxxxx
  const ch = u.pathname.match(/^\/channel\/(UC[\w-]{10,})/);
  if (ch) return { kind: 'channelId', id: ch[1] };

  // /@handle  (or /@handle/anything)
  const handle = u.pathname.match(/^\/@([\w.\-]+)/);
  if (handle) return { kind: 'channelHandle', handle: handle[1] };

  return null;
}
