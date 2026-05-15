import { Client } from 'pg';

// Returns true if SUPABASE_DB_URL is configured. Used to decide whether
// the one-click "Initialize database" path is available.
export function hasDbUrl(): boolean {
  const u = process.env.SUPABASE_DB_URL;
  return typeof u === 'string' && u.length > 0;
}

// Runs a SQL script against Supabase Postgres via the connection string.
// Uses TLS (Supabase requires it) and verifies that the pooler host is
// reachable. Returns nothing on success; throws on failure.
export async function runSql(sql: string): Promise<void> {
  const conn = process.env.SUPABASE_DB_URL;
  if (!conn) throw new Error('SUPABASE_DB_URL is not set');

  const client = new Client({
    connectionString: conn,
    // Supabase requires SSL. We don't pin the CA -- pooler certs rotate.
    ssl: { rejectUnauthorized: false },
  });

  await client.connect();
  try {
    await client.query(sql);
  } finally {
    await client.end();
  }
}
