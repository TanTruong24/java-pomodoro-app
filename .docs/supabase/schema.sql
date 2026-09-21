-- Run once in the Supabase SQL Editor. The desktop app uses only the
-- publishable key and an authenticated user's access token.

create table if not exists public.focus_sessions (
    id uuid primary key,
    user_id uuid not null references auth.users (id) on delete cascade,
    started_at timestamptz not null,
    ended_at timestamptz not null,
    duration_seconds integer not null check (duration_seconds > 0),
    mode text not null check (mode in ('FOCUS', 'SHORT_BREAK'))
);

create index if not exists focus_sessions_user_id_idx
    on public.focus_sessions (user_id);

create table if not exists public.app_settings (
    user_id uuid not null references auth.users (id) on delete cascade,
    setting_key text not null,
    setting_value text not null,
    updated_at timestamptz not null default now(),
    primary key (user_id, setting_key),
    constraint app_settings_known_key check
        (setting_key in ('ui_theme', 'daily_target_minutes', 'timer_settings'))
);

create or replace function public.set_app_setting_updated_at()
returns trigger language plpgsql as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

drop trigger if exists app_settings_updated_at on public.app_settings;
create trigger app_settings_updated_at
before insert or update on public.app_settings
for each row execute function public.set_app_setting_updated_at();

alter table public.focus_sessions enable row level security;
alter table public.app_settings enable row level security;

grant usage on schema public to authenticated;
revoke all on public.focus_sessions from anon, authenticated;
revoke all on public.app_settings from anon, authenticated;
grant select, insert on public.focus_sessions to authenticated;
grant select, insert, update on public.app_settings to authenticated;

drop policy if exists focus_sessions_select_own on public.focus_sessions;
create policy focus_sessions_select_own on public.focus_sessions
for select to authenticated using ((select auth.uid()) = user_id);

drop policy if exists focus_sessions_insert_own on public.focus_sessions;
create policy focus_sessions_insert_own on public.focus_sessions
for insert to authenticated with check ((select auth.uid()) = user_id);

drop policy if exists app_settings_select_own on public.app_settings;
create policy app_settings_select_own on public.app_settings
for select to authenticated using ((select auth.uid()) = user_id);

drop policy if exists app_settings_insert_own on public.app_settings;
create policy app_settings_insert_own on public.app_settings
for insert to authenticated with check ((select auth.uid()) = user_id);

drop policy if exists app_settings_update_own on public.app_settings;
create policy app_settings_update_own on public.app_settings
for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);
