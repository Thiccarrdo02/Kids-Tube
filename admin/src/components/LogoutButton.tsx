'use client';
import { useRouter } from 'next/navigation';

export default function LogoutButton() {
  const router = useRouter();
  return (
    <button
      onClick={async () => {
        await fetch('/api/logout', { method: 'POST' });
        router.push('/login');
        router.refresh();
      }}
      className="text-sm text-gray-600 hover:text-gray-900"
    >
      Sign out
    </button>
  );
}
