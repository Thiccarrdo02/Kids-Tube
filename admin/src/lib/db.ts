import { createClient } from '@supabase/supabase-js';

const url = process.env.SUPABASE_URL;
const key = process.env.SUPABASE_SERVICE_ROLE_KEY;

if (!url || !key) {
  // Allow build to succeed without env, but throw at request time.
  console.warn('[db] SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY not set');
}

export const db = createClient(url ?? 'http://localhost', key ?? 'anon', {
  auth: { persistSession: false },
});

export type Category = {
  id: string;
  name: string;
  sort_order: number;
};

export type Video = {
  video_id: string;
  title: string;
  channel_title: string | null;
  channel_id: string | null;
  thumbnail_url: string | null;
  duration_seconds: number;
  published_at: string | null;
  category_id: string | null;
  source_id: string | null;
  added_at: string;
  is_embeddable: boolean;
};

export type Source = {
  id: string;
  kind: 'playlist' | 'channel';
  source_id: string;
  title: string | null;
  category_id: string | null;
  last_synced_at: string | null;
};
