package com.example.data.repository

import com.example.data.model.FocomDelegate
import com.example.data.model.FocomContent
import com.example.data.model.FocomNews
import com.example.data.model.FocomRight

object FocomDataRepository {

    val realDelegatesList = listOf(
        FocomDelegate(
            id = "ds_1",
            name = "N'deye Yacine SIDIBÉ",
            role = "Secrétaire Générale du Syndicat FOCOM UES ILIAD - Élue Titulaire CSE, Déléguée Syndicale",
            entity = "Free Réseau",
            region = "National",
            email = "nysidibe@focomues-iliad.fr",
            phone = "06.23.29.02.23",
            avatarLetters = "NS",
            photoResName = "delegate_nysidibe"
        ),
        FocomDelegate(
            id = "ds_2",
            name = "Mounir ZERARKA",
            role = "Conducteur de Travaux - Délégué Syndical, Élu Titulaire CSE",
            entity = "Free Mobile",
            region = "National",
            email = "mounir@focomues-iliad.fr",
            phone = "06.50.95.86.66",
            avatarLetters = "MZ",
            photoResName = "delegate_mzerarka"
        ),
        FocomDelegate(
            id = "ds_3",
            name = "Anthony LAVILLE",
            role = "TMF (ex-TFO) - Délégué Syndical",
            entity = "Free Réseau",
            region = "National",
            email = "alaville@focomues-iliad.fr",
            phone = "07.60.15.10.79",
            avatarLetters = "AL",
            photoResName = "delegate_alaville"
        ),
        FocomDelegate(
            id = "ds_4",
            name = "Fabien RACAULT",
            role = "TMF (ex-TFO) - Élu Titulaire CSE, Délégué syndical, Membre CSSCT",
            entity = "ROF",
            region = "National",
            email = "fabien@focomues-iliad.fr",
            phone = "06.31.57.33.42",
            avatarLetters = "FR",
            photoResName = "delegate_fracault"
        ),
        FocomDelegate(
            id = "ds_5",
            name = "Didier BROU",
            role = "CIR (ex-VPI) - Représentant syndical",
            entity = "Free Réseau",
            region = "National",
            email = "didier@focomues-iliad.fr",
            phone = "06.50.54.10.32",
            avatarLetters = "DB",
            photoResName = "delegate_dbrou"
        ),
        FocomDelegate(
            id = "rs_1",
            name = "Fadil KENDIRA",
            role = "TMRE (ex-PDEM) - Délégué syndical, Élu Titulaire CSE, Administrateur du site",
            entity = "Free Réseau",
            region = "National",
            email = "fadil@focomues-iliad.fr",
            phone = "06.50.77.28.25",
            avatarLetters = "FK",
            photoResName = "delegate_fkendira"
        )
    )

    val defaultContent: FocomContent
        get() = FocomContent(
            newsList = newsList,
            rightsList = rightsList,
            delegatesList = realDelegatesList,
            publicationsList = publicationsList
        )

    val projectActualitesFallback = listOf(
        com.example.data.model.FocomPublication(
            id = "project_nao_2026",
            title = "NAO 2026 UES ILIAD : propositions insuffisantes face aux attentes des salariés",
            type = "Négociations",
            date = "2026",
            pdfUrl = "https://focomues-iliad.fr/nao2026",
            excerpt = "Propositions finales de la Direction, position FO et refus de signature.",
            content = "Retrouvez la page NAO 2026 du projet FOCOM UES ILIAD avec les propositions finales, la position FO et les éléments de synthèse."
        ),
        com.example.data.model.FocomPublication(
            id = "project_elections_2026",
            title = "Élections CSE 2026 : résultats et représentation FO COM",
            type = "CSE",
            date = "2026",
            pdfUrl = "https://focomues-iliad.fr/elections",
            excerpt = "Résultats CSE 2026, représentativité et élus FO COM.",
            content = "Retrouvez les résultats des élections CSE 2026, la représentativité FO COM et les informations utiles sur vos élus."
        ),
        com.example.data.model.FocomPublication(
            id = "project_bilan_mandat",
            title = "Bilan CSE 2022-2026 : un mandat d'actions concrètes",
            type = "Bilan",
            date = "2026",
            pdfUrl = "https://focomues-iliad.fr/bilan-mandat",
            excerpt = "Actions, réclamations, accompagnements et bilan du mandat.",
            content = "Consultez le bilan de mandat CSE 2022-2026 et les actions menées pour les salariés de l'UES ILIAD."
        )
    )

    val newsList = listOf(
        FocomNews(
            id = "news_nao_2026",
            title = "NAO 2026 : FO ne signera pas cet accord insuffisant",
            description = "Après quatre réunions, FO dénonce une enveloppe limitée à 1,6 % et l'absence de véritables négociations.",
            content = """POSITION FO COM
La Direction refuse de négocier : FO ne signera pas cet accord insuffisant.

Après quatre réunions de pseudo-négociations, la Direction présente une enveloppe globale limitée à 1,6 %. Pour FO COM, ces propositions ne répondent ni à l'inflation, ni aux attentes légitimes des salariés.

RÉUNIONS NAO 2026
- 15/04/2026 : ouverture des échanges et premières orientations.
- 29/04/2026 : les organisations syndicales portent leurs demandes salariales et sociales.
- 05/05/2026 : la Direction maintient une ligne très limitée malgré les revendications.
- 13/05/2026 : propositions finales, FO dénonce une absence de véritables négociations.

BUDGETS PROPOSÉS PAR LA DIRECTION
- UES ILIAD : 1,6 % dont 1,2 % de révisions salariales et 0,4 % de mesures sociales.
- FREE RÉSEAU / ROF : 1,7 % dont 1,2 % de révisions salariales et 0,5 % de mesures sociales.
- FREE MOBILE : 1,2 % de révisions salariales, aucune mesure sociale dédiée.
- ILIAD / ASSUNET / FREE SAS : 1,2 % de révisions salariales, aucune mesure sociale dédiée.

MESURES SALARIALES
- Demandes OS : enveloppe globale de 2,2 % à 2,9 %.
- Direction : 1,6 % seulement pour l'UES ILIAD.
- Demandes OS : augmentation collective pour tous les salariés.
- Direction : 1 % uniquement pour les packages inférieurs ou égaux à 30 000 €.
- Augmentation automatique après 3 ans sans évolution : refusée.
- Rétroactivité au 1er janvier 2026 : refusée.
- Revalorisation du salaire d'entrée : +240 €/an seulement.

ANCIENNETÉ
- Prime d'ancienneté dès 3 ans puis progression régulière : refusée.
- Congés supplémentaires selon ancienneté : refusés.

TEMPS DE TRAVAIL
- Temps de trajet rémunéré comme temps de travail effectif : refusé.
- Cadres au forfait : réduction de 3 jours ou passage à 39h, négociation promise dans 12 mois.
- Revalorisation des astreintes : refusée.
- Deuxième fenêtre CET : refusée.
- Congés exceptionnels revalorisés : refusés.

TÉLÉTRAVAIL
- 2 jours de télétravail par semaine pour tous : refusé.
- 2 jours consécutifs possibles : refusé.
- Harmonisation UES des règles de télétravail : refusée.
- Réexamen des non-éligibilités : refusé.
- Revalorisation / défiscalisation de la prime domicile : refusée.

TRANSPORT ET MOBILITÉ
- Pass Navigo : prise en charge à 75 %, avancée insuffisante face aux hausses.
- Véhicule de fonction ouvert aux salariés : refusé.
- Prime carburant : 50 € avec conditions très restrictives.

AVANTAGES SOCIAUX
- Tickets restaurant et paniers repas : 9 € pour Free Mobile uniquement.
- Budget convivialité 150 € + 50 € Noël : refusé.
- Prime salissure uniformisée : refusée.
- Prime vacances pour tous : refusée.

ÉGALITÉ FEMMES-HOMMES
- Rattrapage des écarts salariaux femmes-hommes : refusé.
- Congé endométriose supplémentaire : refusé.
- Prise en charge d'un mois du congé naissance collaboratrices : refusée.
- Doublement de la prime cooptation pour les recrutements féminins : refusé.

CSE ET NÉGOCIATIONS COLLECTIVES
- Dotation CSE à 1 % : refusée.
- Communication CSE trimestrielle par email : refusée.
- Assistante sociale siège + régions : refusée.
- Ouverture PPV, IA, intéressement, participation, dialogue social : refusée sauf temps de travail dans 12 mois.
- Réouverture GEPP, égalité pro, CET : refusée.

POURQUOI FO NE SIGNERA PAS
1. Budget global de 1,6 % insuffisant face à l'inflation et aux résultats du Groupe.
2. Augmentation collective de 1 % réservée aux seuls packages inférieurs ou égaux à 30 000 €, excluant une grande partie des salariés.
3. Aucune rétroactivité au 1er janvier 2026 : perte de pouvoir d'achat sur plusieurs mois.
4. Prime carburant de 50 € assortie de conditions tellement restrictives qu'elle concernera très peu de salariés.
5. Aucune avancée réelle sur l'ancienneté, le temps de travail, le télétravail, l'égalité femmes-hommes ou les congés.
6. Absence de véritables négociations : la Direction impose ses propositions sans répondre aux revendications portées par les salariés.

CONCLUSION FO
FO refuse de cautionner cette mascarade et ne signera pas. FO COM reste mobilisé pour défendre le pouvoir d'achat, le dialogue social et des mesures générales, justes et lisibles pour tous les salariés.""",
            date = "13 mai 2026",
            category = "NAO",
            isHotState = true
        ),
        FocomNews(
            id = "news_1",
            title = "Négociations Annuelles Obligatoires (NAO) 2026 : FOCOM revendique",
            description = "FOCOM défend votre pouvoir d'achat face à l'inflation avec des propositions concrètes.",
            content = """Les négociations annuelles obligatoires (NAO) pour l'UES Iliad ont débuté. 
            
FOCOM se bat pour l'ensemble des salariés (Free Réseau, Free Mobile, Iliad, Free SAS, Assunet, ROF) et porte des revendications fortes :
- Une augmentation générale de 4,5% pour tous les salaires inférieurs à 3500€ bruts.
- La revalorisation des tickets restaurant à 10€ (avec prise en charge à 60% par l'employeur).
- Une prime de partage de la valeur (PPV) de 1500€ pour soutenir les bas salaires.
- L'harmonisation des grilles de salaire minimum entre les différentes entités du groupe.

Votre mobilisation est notre force lors des tables rondes avec la direction. Restons unis !""",
            date = "22 mai 2026",
            category = "NAO",
            isHotState = false
        ),
        FocomNews(
            id = "news_2",
            title = "Élections professionnelles CSE : Victoire pour la représentativité de FOCOM !",
            description = "Grâce à vous, FOCOM renforce sa présence dans les instances pour mieux vous représenter.",
            content = """Les résultats des élections du Comité Social et Économique (CSE) viennent de tomber ! 

Grâce à votre vote et votre confiance renouvelée, FOCOM enregistre une progression remarquable historique de +6% sur l'ensemble de l'UES Iliad. Nous obtenons de nouveaux sièges d'élus chez Free Réseau, Free Mobile et Free SAS.

Vos élus FOCOM s'engagent à :
- Surveiller de près la santé financière de l'entreprise et l'emploi.
- Améliorer les œuvres sociales du CSE (chèques vacances, billetterie, cadeaux de fin d'année).
- Être présents sur le terrain au quotidien pour répondre à vos questions et régler vos litiges.

Merci pour votre soutien, le combat continue pour préserver et conquérir de nouveaux droits de haute valeur !""",
            date = "15 mai 2026",
            category = "CSE",
            isHotState = false
        ),
        FocomNews(
            id = "news_3",
            title = "Télétravail : Vers un nouvel accord plus flexible et mieux indemnisé",
            description = "FOCOM négocie l'élargissement du télétravail à 3 jours par semaine.",
            content = """La charte télétravail actuellement en vigueur chez Iliad arrive à échéance. FOCOM a demandé l'ouverture de négociations pour aboutir à un accord d'entreprise solide et plus favorable pour les entités Free Mobile et Assunet notamment.

Nos principales propositions :
- Porter la possibilité de télétravail jusqu'à 3 jours par semaine pour tous les postes éligibles.
- Instaurer une enveloppe d'indemnisation forfaitaire mensuelle de 30€ (frais d'électricité, de chauffage et internet).
- Un budget d'équipement initial de 120€ pour l'achat de matériel d'ergonomie (siège, écran, repose-pieds).
- Un droit à la déconnexion fermement respecté avec des coupures de serveurs automatiques hors des plages d'activité.

Les retours de la direction sont attendus sous peu. Nous vous tiendrons informés d'urgence !""",
            date = "08 mai 2026",
            category = "Accords",
            isHotState = false
        ),
        FocomNews(
            id = "news_4",
            title = "Épargne salariale : Tout savoir sur la participation et l'intéressement UES Iliad",
            description = "Le calendrier de versement approche. Comment arbitrer au mieux vos primes ?",
            content = """L'enveloppe globale de participation et d'intéressement au titre des résultats de l'exercice a été calculée.

Voici les grandes dates à retenir pour arbitrer vos choix de placement :
1. Réception de la fiche d'arbitrage individuelle par e-mail début juin.
2. Date limite de réponse : 18 juin 2026.
3. Versement effectif sur votre compte bancaire ou placement sur le PEE/PERCO fin juin.

FOCOM vous conseille :
- Si vous avez un besoin urgent d'argent : vous pouvez demander le versement direct, mais attention, cette somme sera imposable sur le revenu.
- Pour une épargne d'avenir : le placement sur le PEE ou le PERCO bénéficie d'une exonération fiscale d'impôt sur le revenu et permet de faire grandir votre épargne (avec abonde éventuel selon les accords d'entité).

N'hésitez pas à solliciter un représentant FOCOM de votre établissement pour vous aider dans vos démarches.""",
            date = "30 avril 2026",
            category = "Vie Syndicale",
            isHotState = false
        )
    )

    val rightsList = listOf(
        FocomRight(
            id = "right_1",
            title = "Télétravail et Charte Groupe",
            category = "Télétravail",
            summary = "Quels sont les postes éligibles et les indemnités prévues par le protocole ?",
            detailMarkdown = """Le télétravail dans l'UES Iliad est régi par un avenant à l'accord d'entreprise.

### Éligibilité :
Tous les salariés en CDI ou CDD (hors alternants en première année) dont l'activité est compatible ont accès à un forfait de télétravail à hauteur de 2 jours par semaine (parfois 3 selon l'entité).

### Prise en charge par l'entreprise :
- Fourniture d'un ordinateur portable, d'un écran externe de 24 pouces, d'un casque audio et d'un clavier/souris.
- Indemnité forfaitaire de télétravail de 15€ par mois pour 2 jours par semaine.

### Vos devoirs et droits :
Vos horaires restent strictement identiques à ceux effectués sur site. Aucun contrôle abusif de présence ou enregistrement audio/vidéo continu n'est autorisé. Vous disposez du droit légal de ne pas répondre aux mails en dehors des heures définies.""",
            focomAdvice = "FOCOM milite pour une revalorisation de l'indemnité à 30€/mois pour compenser la hausse de l'électricité et du gaz chez vous lors de vos journées de télétravail."
        ),
        FocomRight(
            id = "right_2",
            title = "Congés d'ancienneté conventionnels",
            category = "Congés",
            summary = "Découvrez le calcul de vos jours de congés supplémentaires d'ancienneté.",
            detailMarkdown = """Selon la convention collective nationale des Télécommunications (CCNT Télécoms) applicable dans l'UES Iliad, l'ancienneté ouvre droit à des jours de congés payés supplémentaires.

### Jours d'ancienneté :
- **De 1 à moins de 5 ans d'ancienneté :** pas de jour supplémentaire.
- **De 5 à moins de 10 ans d'ancienneté :** 1 jour supplémentaire de congé par an.
- **De 10 à moins de 15 ans d'ancienneté :** 2 jours supplémentaires par an.
- **Plus de 15 ans d'ancienneté :** 3 jours supplémentaires par an.

### Droits exceptionnels pour évènements familiaux :
- Mariage ou PACS du salarié : 5 jours de congés.
- Naissance ou adoption : 3 jours.
- Mariage d'un enfant : 1 jour.
- Décès d'un proche (parent, enfant, conjoint) : de 3 à 7 jours d'absence rémunérée.""",
            focomAdvice = "Certaines entités du groupe essaient de regrouper ces jours de façon défavorable. Si vous constatez un oubli sur votre fiche de paie (solde CP/RTT), prévenez immédiatement FOCOM."
        ),
        FocomRight(
            id = "right_3",
            title = "Complémentaire Santé & Prévoyance",
            category = "Santé",
            summary = "Garanties d'hospitalisation, de soins optiques, dentaires et taux d'indemnisation.",
            detailMarkdown = """La mutuelle d'entreprise d'Iliad est obligatoire pour tous les salariés, sauf cas de dispense légitime (conjoint déjà affilié, CDD court).

### Niveaux de remboursement :
- **Consultations générales ou de spécialistes :** Prise en charge de 100% à 150% de la base de remboursement de la Sécurité sociale.
- **Optique :** Forfaits avantageux renouvelables tous les 2 ans pour les montures et les verres complexes (jusqu'à 350€ selon les options).
- **Dentaire :** Excellent remboursement pour les couronnes et les soins courants.

### Prévoyance Invalidité-Décès :
Elle assure le maintien de salaire à 100% (complément de la sécurité sociale de prévoyance) pendant une durée de 90 jours en cas d'arrêt maladie continu.""",
            focomAdvice = "FOCOM est représentée au conseil de surveillance de la mutuelle groupe. Nous pesons de tout notre poids pour bloquer la hausse annuelle des cotisations salariales."
        ),
        FocomRight(
            id = "right_4",
            title = "Participation et Intéressement",
            category = "Rémunération",
            summary = "Formule de calcul, déblocage anticipé et abondement versé par l'employeur.",
            detailMarkdown = """La redistribution de la valeur se fait par deux mécanismes chez Iliad : la participation légale et l'intéressement d'entreprise.

### L'intéressement :
Il est basé sur l'atteinte d'objectifs opérationnels définis par entité (ex : taux de réponse au support chez Centrapel, respect des plannings de déploiement de fibre chez Free Réseau).

### La participation :
Elle découle directement du bénéfice net consolidé du groupe Iliad en France.

### Abondement :
Selon l'accord de votre entreprise, si vous décidez d'y placer vos sommes reçues plutôt que de les toucher immédiatement sur votre compte bancaire, l'employeur verse un abondement (une somme cadeau en plus) pouvant aller jusqu'à 10 ou 15% de votre dépôt pour vous encourager.""",
            focomAdvice = "Nous exigeons un calcul plus juste et solidaire de l'intéressement qui ne pénalise pas les salariés malades ou en congé maternité durant l'année."
        ),
        FocomRight(
            id = "right_5",
            title = "Droit de Grève (FAQ de survie)",
            category = "Législation",
            summary = "Puis-je faire grève tout seul ? Y a-t-il un préavis ? Retenu sur salaire.",
            detailMarkdown = """Le droit de grève est un droit fondamental garanti par la Constitution française (Article 7 du préambule de la Constitution de 1946).

### 1. Puis-je faire grève tout seul dans mon coin ?
Dans le secteur privé (comme l'UES Iliad), vous ne pouvez pas faire grève seul, sauf si vous rejoignez un ordre d'appel national ou si vous êtes l'unique salarié de l'établissement à cesser le travail dans le cadre d'un mouvement collectif national initié par un syndicat comme FOCOM. Il faut au moins deux salariés mobilisés.

### 2. Le préavis obligatoire :
Contrairement au secteur public, il n'y a pas de préavis obligatoire dans le secteur privé ! Vous pouvez cesser le travail à tout moment, dès lors que l'employeur a connaissance de vos revendications professionnelles au moment de l'arrêt du travail.

### 3. Retenue sur salaire :
L'employeur peut retenir une fraction de salaire strictement proportionnelle à la durée de l'arrêt de travail. Toute retenue disproportionnée ou sanction disciplinaire est interdite par la loi.""",
            focomAdvice = "FOCOM vous conseille de toujours informer vos délégués de proximité avant une action collective, afin d'assurer la protection juridique de votre mouvement."
        ),
        FocomRight(
            id = "right_6",
            title = "Droit d'Alerte et Droit de Retrait",
            category = "Sécurité",
            summary = "Qu'est-ce que le droit de retrait ? Procédure en cas de burn-out ou harcèlement.",
            detailMarkdown = """Chaque travailleur dispose du droit de se retirer d'une situation de travail présentant un danger grave et imminent pour sa vie ou sa santé.

### Le Droit de Retrait (Art. L. 4131-1 et suivants):
- **Condition :** Vous devez avoir un motif raisonnable de penser que la situation présente un danger grave et immédiat (ex: matériel électrique défectueux sous tension, agression, température glaciale insupportable).
- **Conséquence :** Aucune retenue de salaire ni sanction ne peut être infligée. L'employeur ne peut pas vous demander de reprendre le travail avant d'avoir sécurisé le poste.

### Que faire en cas de Burn-Out ou Harcèlement ?
Le droit d'alerte peut être déclenché par un représentant FOCOM au CSE si un danger physique ou psychosocial (harcèlement, pressions managériales abusives) est constaté sur un salarié.""",
            focomAdvice = "Si vous vous sentez en danger ou menacé d'effondrement psychologique, n'attendez pas ! Exercez votre droit de retrait et écrivez d'urgence à un délégué FOCOM pour être accompagné."
        )
    )

    val delegatesList = listOf(
        FocomDelegate(
            id = "ds_1",
            name = "N'deye Yacine Sidibé",
            role = "Déléguée Syndicale Centrale",
            entity = "Free Réseau",
            region = "National",
            email = "ny.sidibe.focom@gmail.com",
            phone = "06 00 00 00 01",
            avatarLetters = "NS"
        ),
        FocomDelegate(
            id = "ds_2",
            name = "Mounir Zerarka",
            role = "Délégué Syndical",
            entity = "Free Mobile",
            region = "National",
            email = "m.zerarka.focom@gmail.com",
            phone = "06 00 00 00 02",
            avatarLetters = "MZ"
        ),
        FocomDelegate(
            id = "ds_3",
            name = "Fabien Racault",
            role = "Délégué Syndical",
            entity = "ROF",
            region = "National",
            email = "f.racault.focom@gmail.com",
            phone = "06 00 00 00 03",
            avatarLetters = "FR"
        ),
        FocomDelegate(
            id = "ds_4",
            name = "Anthony Laville",
            role = "Délégué Syndical",
            entity = "Free Réseau",
            region = "National",
            email = "a.laville.focom@gmail.com",
            phone = "06 00 00 00 04",
            avatarLetters = "AL"
        ),
        FocomDelegate(
            id = "ds_5",
            name = "Fadil Kendira",
            role = "Délégué Syndical",
            entity = "Free Réseau",
            region = "National",
            email = "f.kendira.focom@gmail.com",
            phone = "06 00 00 00 05",
            avatarLetters = "FK"
        ),
        FocomDelegate(
            id = "rs_1",
            name = "Didier Brou",
            role = "Représentant Syndical",
            entity = "Free Réseau",
            region = "National",
            email = "d.brou.focom@gmail.com",
            phone = "06 00 00 00 06",
            avatarLetters = "DB"
        )
    )

    val publicationsList = listOf(
        com.example.data.model.FocomPublication(
            id = "pub_1",
            title = "Le Journal FOCOM - Mai 2026",
            type = "Journal",
            date = "Mai 2026",
            pdfUrl = "https://focom-iliad.fr/journal_mai_2026.pdf"
        ),
        com.example.data.model.FocomPublication(
            id = "pub_2",
            title = "Tract NAO 2026 : Nos Revendications",
            type = "Tract",
            date = "22 Mai 2026",
            pdfUrl = "https://focom-iliad.fr/tract_nao_2026.pdf"
        ),
        com.example.data.model.FocomPublication(
            id = "pub_3",
            title = "Guide du Télétravail Iliad",
            type = "Affiche",
            date = "Janvier 2026",
            pdfUrl = "https://focom-iliad.fr/guide_teletravail.pdf"
        )
    )
}
