import Link from 'next/link';
import { redirect } from 'next/navigation';
import { headers } from 'next/headers';
import { getSession } from '@/lib/auth';
import { db } from '@/lib/db';
import LogoutButton from '@/components/LogoutButton';

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session.loggedIn) redirect('/login');

  // If the database isn't initialized yet, force the user through /setup
  // before anything else. The setup page itself is exempt to avoid loops.
  const pathname = headers().get('x-pathname') ?? '';
  if (!pathname.startsWith('/dashboard/setup')) {
    const { error } = await db
      .from('categories')
      .select('id', { count: 'exact', head: true });
    if (error) redirect('/dashboard/setup');
  }

  return (
    <div className="min-h-screen">
      <header className="bg-white border-b">
        <div className="max-w-5xl mx-auto px-4 py-3 flex items-center gap-4">
          <Link href="/dashboard" className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-md bg-brand grid place-items-center">
              <svg viewBox="0 0 24 24" width="12" height="12" fill="white"><path d="M8 5v14l11-7z"/></svg>
            </div>
            <span className="font-semibold">KidsTube</span>
          </Link>
          <nav className="flex gap-3 text-sm text-gray-600 ml-2">
            <Link href="/dashboard" className="hover:text-gray-900">Videos</Link>
            <Link href="/dashboard/add" className="hover:text-gray-900">Add</Link>
            <Link href="/dashboard/categories" className="hover:text-gray-900">Categories</Link>
            <Link href="/dashboard/sources" className="hover:text-gray-900">Sources</Link>
          </nav>
          <div className="ml-auto"><LogoutButton /></div>
        </div>
      </header>
      <main className="max-w-5xl mx-auto p-4">{children}</main>
    </div>
  );
}
