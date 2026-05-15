import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

// Surfaces the current pathname to server components via the request
// headers. Layouts can't read pathname directly in App Router; cloning
// the incoming Headers and adding x-pathname is the documented workaround.
export function middleware(req: NextRequest) {
  const requestHeaders = new Headers(req.headers);
  requestHeaders.set('x-pathname', req.nextUrl.pathname);
  return NextResponse.next({ request: { headers: requestHeaders } });
}

export const config = {
  matcher: ['/dashboard/:path*'],
};
