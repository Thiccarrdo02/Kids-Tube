import Link from 'next/link';
import { redirect } from 'next/navigation';
import { getSession } from '@/lib/auth';
import LogoutButton from '@/components/LogoutButton';

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session.loggedIn) redirect('/login');
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
          </nav>
          <div className="ml-auto"><LogoutButton /></div>
        </div>
      </header>
      <main className="max-w-5xl mx-auto p-4">{children}</main>
    </div>
  );
}
