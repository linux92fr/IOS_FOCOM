# Messagerie Android et API Supabase existante

L'application Android envoie les messages vers la table existante du site web :

```sql
public.contact_messages
```

Schéma attendu par le site :

```sql
name text not null
email text not null
subject text not null
message text not null
category text not null default 'Général'
status text not null default 'non-lu'
```

L'app Android renseigne `category = 'Application Android'`.

À vérifier côté Supabase :

```sql
alter table public.contact_messages enable row level security;

create policy "Anyone can send a contact message"
on public.contact_messages
for insert
to anon, authenticated
with check (true);

grant insert on table public.contact_messages to anon, authenticated;
```

Le client Android doit utiliser uniquement :

```env
SUPABASE_URL=https://TON_PROJECT_REF.supabase.co
SUPABASE_PUBLISHABLE_KEY=TA_CLE_PUBLISHABLE_OU_ANON
```

Ne jamais mettre de clé `service_role` dans l'application mobile.
