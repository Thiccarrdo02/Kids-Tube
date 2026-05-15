# KidsTube

A personal "YouTube for kids" — the parent curates exactly which videos are
allowed on a web admin panel; the kids see them in an Android app that looks
and feels like YouTube. Sideloaded only. Not for app stores.

```
KIDSTUBE/
├── admin/      Next.js 14 admin panel (deploys to Vercel)
├── android/    Native Android app (Kotlin + Jetpack Compose)
└── .github/    CI workflow that builds the debug APK on each push
```

## Stack choices (and why)

**Admin panel**
- **Next.js 14 (App Router) + TypeScript** — UI and API routes in one project, one-click Vercel deploy, free tier is more than enough.
- **Supabase (Postgres) free tier** — survives deploys, simple SQL, generous quota.
- **Tailwind CSS** — mobile-responsive without bespoke CSS; the parent can edit a class name to tweak the look.
- **iron-session** — battle-tested signed-cookie auth, no JWT machinery.

**Android app**
- **Kotlin + Jetpack Compose** — Google's current native stack; smooth 60fps lists, no cross-platform tax.
- **android-youtube-player** (PierfrancescoSoffritti) — the most maintained YouTube IFrame Player wrapper for Android. Compliant with YouTube's TOS because it embeds the same IFrame player the website uses. We disable related videos, annotations, and the "Watch on YouTube" link.
- **Retrofit + Moshi**, **Coil** for images, **DataStore Preferences** for local state — boring, well-supported choices.
- **Min SDK 24** (Android 7.0) — covers ~95% of in-use devices.

YouTube API usage uses only `playlistItems.list`, `videos.list`, `playlists.list`, and `channels.list` (1 quota unit each). `search.list` (100 units) is never called. A "Refresh sources" cycle on, say, 10 saved sources costs roughly 20-30 units.

---

## 1. Get a YouTube Data API key

1. Open [https://console.cloud.google.com](https://console.cloud.google.com) and create a new project (or pick an existing one).
2. In the sidebar, go to **APIs & Services → Library**, search for **YouTube Data API v3**, click **Enable**.
3. Go to **APIs & Services → Credentials → Create credentials → API key**. Copy the key.
4. (Optional but recommended) Click the key → **Application restrictions: None**, **API restrictions:** select only "YouTube Data API v3". Save.

The free quota is 10,000 units/day, which is far more than this app uses.

## 2. Create the Supabase database

1. Sign up at [https://supabase.com](https://supabase.com) (free).
2. Create a new project. Pick any region near you. Save the database password somewhere.
3. Once it's ready, go to **Settings → API** and copy:
   - **Project URL** → this is your `SUPABASE_URL`
   - **service_role** key (the secret one, **not** anon) → this is your `SUPABASE_SERVICE_ROLE_KEY`
4. Go to **SQL Editor → + New query**, paste the contents of [`admin/supabase-schema.sql`](admin/supabase-schema.sql), and click **Run**. This creates the tables and the default categories.

## 3. Deploy the admin panel to Vercel

1. Push this repo to your GitHub account.
2. Go to [https://vercel.com/new](https://vercel.com/new), import the repo.
3. **Important**: set **Root Directory** to `admin/` (since the admin lives in a subfolder).
4. Framework preset will auto-detect as Next.js. Leave build/output commands at defaults.
5. Add the following **Environment Variables** (all under "Production" and "Preview"):

   | Name | Value |
   | --- | --- |
   | `ADMIN_PASSWORD` | Any password you want for logging into the admin |
   | `SESSION_SECRET` | At least 32 random characters. Generate with `openssl rand -base64 32` |
   | `YOUTUBE_API_KEY` | The key from step 1 |
   | `SUPABASE_URL` | From step 2 |
   | `SUPABASE_SERVICE_ROLE_KEY` | From step 2 |

6. Click **Deploy**. After ~1 minute you'll get a URL like `https://kidstube-admin-xxxx.vercel.app`. That URL is your **backend URL** — you'll need it for the Android app.

## 4. Log in and add your first video

1. Visit your Vercel URL, enter the `ADMIN_PASSWORD` you set.
2. Click **Add**, paste any of:
   - A single video URL: `https://www.youtube.com/watch?v=...`
   - A playlist URL: `https://www.youtube.com/playlist?list=...`
   - A channel URL: `https://www.youtube.com/@SomeHandle`
3. Click **Look up** — you'll see a preview card with the thumbnail, title, and item count.
4. Pick a category (the defaults are Islamic, Educational, Stories, Cartoons, Other; manage them from **Categories**), click **Confirm**.

Use **Refresh sources** on the Videos page to pick up new uploads on saved playlists or channels.

## 5. Build the APK

You have two options.

### Option A — Build automatically via GitHub Actions (recommended)

1. Edit [`android/config.properties`](android/config.properties) and set `BACKEND_URL=` to your Vercel URL (the one you got in step 3). Commit and push.
2. The workflow at [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml) runs on every push to `main`.
3. When it finishes, open the **Actions** tab → latest run → download the **`kidstube-debug-apk`** artifact, or grab it from the rolling **`latest`** GitHub Release the workflow attaches it to.

You can also trigger it manually from **Actions → Build debug APK → Run workflow**, optionally passing a one-off `backend_url` input that overrides `config.properties`.

### Option B — Build locally

You need JDK 17 and the Android SDK (Android Studio is the easy way).

```bash
cd android
# Edit config.properties so BACKEND_URL points at your Vercel URL.
./gradlew assembleDebug
# APK lands at: android/app/build/outputs/apk/debug/app-debug.apk
```

The build constant `BuildConfig.BACKEND_URL` is read from `config.properties` at build time. You can also override per-build with the env var `BACKEND_URL=https://... ./gradlew assembleDebug`.

## 6. Install on the kid's device

1. Transfer `app-debug.apk` to the device (Google Drive, USB, AirDrop to a Mac+phone, etc).
2. On the device: open the APK file. Android will ask you to allow installs from this source — say yes, then tap Install.
3. Launch **KidsTube**. On first run it will pull the video list from your admin backend.

If the backend URL ever changes, you don't need to rebuild: long-press the **KidsTube** logo at the top of the home screen 5 times, then enter the PIN you set (you'll be asked to set one the first time). From there you can change the backend URL, clear cache, or force a refresh.

---

## Day-to-day maintenance

| You want to... | Where to look |
| --- | --- |
| Change the brand color | `android/app/src/main/java/com/family/kidstube/ui/theme/Theme.kt` (`BrandRed`) and `admin/tailwind.config.ts` (`brand`) |
| Rename or add a category | Admin → **Categories** tab |
| Change the cache TTL | `FeedRepository.kt` (`TTL_MS`) |
| Change watch-history size | `AppPrefs.kt` (`take(50)`) |
| Move the admin to a new URL | Vercel deploy + update each device's backend URL via parental settings |
| Rotate the YouTube API key | Update `YOUTUBE_API_KEY` in Vercel env vars, redeploy |

## How the two pieces talk

The Android app calls a single endpoint:

```
GET <BACKEND_URL>/api/public/feed
```

That returns JSON like:

```json
{
  "version": 1,
  "generatedAt": "2026-05-15T...",
  "categories": [{ "id": "...", "name": "Islamic", "sortOrder": 1 }, ...],
  "videos": [
    {
      "id": "dQw4w9WgXcQ",
      "title": "...",
      "channelTitle": "...",
      "thumbnailUrl": "https://i.ytimg.com/...",
      "durationSeconds": 213,
      "publishedAt": "2024-...",
      "categoryId": "...",
      "addedAt": "..."
    }
  ]
}
```

No API key is on the device. All YouTube API work happens on the backend.

## Privacy & safety

- No analytics, no crash reporting, no telemetry. The app makes exactly two kinds of network calls: to your admin backend, and to YouTube's IFrame player (to play the videos).
- No login, no comments, no share, no related-videos overlay, no "Watch on YouTube" link.
- Parental settings are gated behind a 4-digit PIN (SHA-256 hashed on device — not stored as plaintext).
- The app only navigates to YouTube embed URLs; external intents are not handled.
- HTTPS is enforced (`usesCleartextTraffic="false"` in the manifest).
