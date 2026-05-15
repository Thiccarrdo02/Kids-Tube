// Inlined version of supabase-schema.sql so the /api/setup route can
// execute it without reading from disk (Vercel functions don't ship the
// .sql file alongside).
export const SCHEMA_SQL = `
create table if not exists categories (
  id uuid primary key default gen_random_uuid(),
  name text unique not null,
  sort_order int not null default 0,
  created_at timestamptz not null default now()
);

create table if not exists sources (
  id uuid primary key default gen_random_uuid(),
  kind text not null check (kind in ('playlist','channel')),
  source_id text unique not null,
  title text,
  category_id uuid references categories(id) on delete set null,
  last_synced_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists videos (
  video_id text primary key,
  title text not null,
  channel_title text,
  channel_id text,
  thumbnail_url text,
  duration_seconds int default 0,
  published_at timestamptz,
  category_id uuid references categories(id) on delete set null,
  source_id uuid references sources(id) on delete set null,
  added_at timestamptz not null default now()
);

create index if not exists videos_added_at_idx on videos (added_at desc);
create index if not exists videos_category_idx on videos (category_id);

insert into categories (name, sort_order) values
  ('Islamic', 1),
  ('Educational', 2),
  ('Stories', 3),
  ('Cartoons', 4),
  ('Other', 99)
on conflict (name) do nothing;

-- Force PostgREST (Supabase's REST gateway) to reload its schema cache
-- immediately. Without this the first /api/categories call right after
-- setup can still hit "table not found in schema cache".
notify pgrst, 'reload schema';
`;
