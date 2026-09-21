# Supabase sync setup

1. Create a Supabase project. In **SQL Editor**, run [`schema.sql`](schema.sql).
2. Create one email/password account in **Authentication > Users**, or enable email sign-up and create an account there. Sign in with that same account on every computer.
3. Copy [`supabase.properties.example`](supabase.properties.example) to `../../data/supabase.properties` in the app data directory and replace the two placeholders. Use the **Project URL** and **publishable key** from the Supabase dashboard. Never use a secret or service-role key in a desktop app.
4. Start the app and click **Sign in**. Subsequent starts refresh the locally saved login and sync automatically. **Sync now** retries manually. Normal app close also makes a bounded upload attempt.

The app data directory is `%LOCALAPPDATA%\Pomodoro` on Windows, `$XDG_DATA_HOME/pomodoro` when `XDG_DATA_HOME` is set on Linux, and `~/.pomodoro` otherwise. You can instead set the `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` environment variables; each overrides the corresponding value in the properties file. Both values must be available.

The same directory holds `pomodoro.db` and `supabase-session.properties`. The session file contains a refresh token. Protect the directory as private user data and do not commit either file. The app associates one local database with one Supabase user; use a separate OS user profile or data directory for another account.

Completed focus and short-break sessions, the theme, daily target, and timer durations are synced. Running/paused timer state is local only. Sessions use UUIDs and are uploaded idempotently. Settings use **last successful server write wins** if two devices change the same key. When offline, changes stay in SQLite and the next successful sync retries them.

The first sync uploads existing local records and downloads remote records. Later syncs upload only unsynced local records but read all remote sessions in pages. This is suitable for a personal Pomodoro history; a large shared history would benefit from a server-side change cursor.

Official references: [Supabase Data API](https://supabase.com/docs/guides/api), [API keys](https://supabase.com/docs/guides/getting-started/api-keys), and [Row Level Security](https://supabase.com/docs/guides/database/postgres/row-level-security).
