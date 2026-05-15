import { NextResponse } from 'next/server';
import { db } from '@/lib/db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

// Public read-only feed for the Android app. No auth. CORS open.
// Shape is stable -- the Android client deserializes this directly.
export async function GET() {
  const [{ data: videos, error: ve }, { data: cats, error: ce }] = await Promise.all([
    // Only serve videos that YouTube allows us to embed. Refresh sources
    // updates this flag periodically.
    db.from('videos').select('*').eq('is_embeddable', true).order('added_at', { ascending: false }),
    db.from('categories').select('*').order('sort_order'),
  ]);
  if (ve) return jsonCors({ error: ve.message }, 500);
  if (ce) return jsonCors({ error: ce.message }, 500);

  return jsonCors({
    version: 1,
    generatedAt: new Date().toISOString(),
    categories: (cats ?? []).map(c => ({ id: c.id, name: c.name, sortOrder: c.sort_order })),
    videos: (videos ?? []).map(v => ({
      id: v.video_id,
      title: v.title,
      channelTitle: v.channel_title,
      channelId: v.channel_id,
      thumbnailUrl: v.thumbnail_url,
      durationSeconds: v.duration_seconds,
      publishedAt: v.published_at,
      categoryId: v.category_id,
      addedAt: v.added_at,
    })),
  });
}

export async function OPTIONS() {
  return new NextResponse(null, { status: 204, headers: corsHeaders() });
}

function corsHeaders() {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
    'Cache-Control': 'public, max-age=60',
  };
}

function jsonCors(body: any, status = 200) {
  return NextResponse.json(body, { status, headers: corsHeaders() });
}
