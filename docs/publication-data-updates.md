# Mise à jour des données après publication Play Store

L'application peut être publiée une seule fois sur le Play Store, puis afficher des contenus mis à jour sans nouvelle APK si les données viennent de Supabase ou du JSON distant GitHub.

## Actualités / Presse

L'onglet Presse lit la table Supabase existante du site :

```sql
public.articles
```

Requête utilisée côté app :

```text
is_published = true
order by published_at desc nulls last
```

Pour qu'un article apparaisse dans l'app :

1. le publier dans l'admin du site ;
2. vérifier `is_published = true` ;
3. renseigner `published_at` ;
4. ouvrir l'app ou appuyer sur le bouton d'actualisation dans Presse.

À vérifier côté Supabase :

```sql
alter table public.articles enable row level security;

create policy "Public can read published articles"
on public.articles
for select
to anon, authenticated
using (is_published = true);

grant select on table public.articles to anon, authenticated;
```

## Données éditoriales Android

Le reste du contenu peut rester synchronisé via :

```text
https://raw.githubusercontent.com/linux92fr/focom-ues-iliad2/main/data/focom-content.json
```

Modifier ce fichier côté dépôt mettra à jour l'app au prochain chargement, sans publication Play Store.

## Quand faut-il republier l'APK ?

Nouvelle APK nécessaire uniquement pour :

- changer le design ou la navigation ;
- ajouter une nouvelle fonctionnalité native ;
- modifier le schéma local Room ;
- corriger un bug applicatif.

Pas nécessaire pour :

- publier une actualité ;
- modifier un article ;
- ajouter un tract/document si le flux Supabase le fournit ;
- changer les textes ou listes distantes.
