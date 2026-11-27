# FOCOM UES ILIAD Android

Application Android officielle FOCOM UES ILIAD.

L'application permet de consulter les actualites syndicales, les droits salaries, les representants FOCOM, ainsi que d'envoyer un message au syndicat via la messagerie interne connectee a Supabase.

## Prerequis

- Android Studio
- JDK integre a Android Studio
- Un projet Supabase pour les actualites et les messages de contact
- Une cle de signature release pour publier sur Google Play

## Configuration locale

Creer un fichier `.env` a la racine du projet a partir de `.env.example`.

Les valeurs Supabase attendues sont :

```env
SUPABASE_URL=https://votre-projet.supabase.co
SUPABASE_PUBLISHABLE_KEY=votre_cle_publishable_ou_anon
```

Ne jamais publier de cle secrete ou de service role dans ce depot.

## Lancer l'application

Depuis Android Studio :

1. Ouvrir le dossier du projet.
2. Attendre la synchronisation Gradle.
3. Selectionner un emulateur ou un telephone Android.
4. Lancer la configuration `app`.

Depuis PowerShell :

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
```

L'APK debug est genere dans :

```text
app\build\outputs\apk\debug\
```

## Generer un bundle Google Play

Avant chaque nouvelle publication, augmenter `versionCode` dans :

```text
app\build.gradle.kts
```

Exemple :

```kotlin
versionCode = 5
versionName = "1.0.4"
```

Puis generer l'Android App Bundle :

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:bundleRelease
```

Le fichier AAB est genere dans :

```text
app\build\outputs\bundle\release\app-release.aab
```

## Signature

La publication Google Play doit toujours utiliser la meme cle de signature.

Verifier l'empreinte SHA1 d'un keystore :

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
& "$env:JAVA_HOME\bin\keytool.exe" -list -v -keystore ".\focom-release-key" -alias "focom_key"
```

## Donnees distantes

Les actualites et certains contenus peuvent etre charges depuis Supabase ou depuis le depot web FOCOM UES ILIAD.

Si les donnees distantes sont indisponibles, l'application conserve des contenus de secours embarques afin de rester utilisable.
