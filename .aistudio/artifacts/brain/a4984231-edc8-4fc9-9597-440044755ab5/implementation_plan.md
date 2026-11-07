# Calendrier Interactif du Journal & Total Cumulé des Cigarettes

Intégration d'une vue Calendrier interactive pour le Journal avec navigation mensuelle et affichage instantané de l'historique du jour sélectionné (avec bascule vers la vue Liste), et mise en valeur du total cumulé de cigarettes fumées sur le Journal et l'onglet Progrès.

## Décisions Clés & Réponses Confirmées

> [!IMPORTANT]
> Les choix d'ergonomie et d'affichage suivants ont été validés avec l'utilisateur :
> - **Format du Journal** : Vue Calendrier par défaut avec bouton/onglet pour basculer facilement en vue Liste.
> - **Affichage de la date sélectionnée** : Directement sous le calendrier sur la même page (accès immédiat aux détails, cigarettes, intervalles et boutons de confirmation).
> - **Total cumulé des cigarettes fumées** : Affiché en haut de l'écran Journal ainsi que dans la synthèse de l'onglet Progrès.

---

## 1. Vue d'ensemble & Bénéfices Utilisateur

- **Visualisation temporelle globale** : Le calendrier offre une vue claire du mois calendaire, avec des repères visuels par jour (pastille de statut, nombre de cigarettes fumées, code couleur pour les journées sans fumer ou sous le plafond).
- **Consultation ciblée au toucher** : Cliquer sur n'importe quel jour (aujourd'hui, hier ou une date antérieure) charge instantanément sous le calendrier la liste chronologique des cigarettes avec l'heure exacte, le contexte, l'intensité, le bouton de modification/suppression et les intervalles intra-journée.
- **Flexibilité** : Possibilité de passer en 1 clic de la vue Calendrier à la vue Liste habituelle (groupes ordonnés par jour) selon la préférence d'usage.
- **Suivi global du volume fumé** : Mise en évidence du total absolu de cigarettes enregistrées dans l'application, en haut du Journal et dans les cartes de progrès.

---

## 2. Expérience Utilisateur & Design Visuel

### A. Total cumulé des cigarettes
- **En haut du Journal** : Une carte/bannière d'en-tête dynamique affichant le total cumulé de cigarettes enregistrées avec une icône Material 3 élégante (`SmokingRooms` ou `Analytics`), le nombre de jours suivis et le stock restant.
- **Dans l'onglet Progrès** : Une nouvelle carte métrique dédiée au total global fumé, positionnée aux côtés du total d'économies en TND et du nombre de journées confirmées sans fumer.

### B. Contrôle de bascule (Vue Calendrier vs Vue Liste)
- Un sélecteur à deux segments (`SingleChoiceSegmentedButtonRow` ou deux boutons stylisés) :
  - `📅 Calendrier` (sélectionné par défaut)
  - `📋 Liste chronologique`
- Animation fluide lors du changement de mode.

### C. Vue Calendrier interactive
- **En-tête de navigation du mois** :
  - Bouton chevron précédent `<`
  - Mois et année en français (ex. « Novembre 2026 »)
  - Bouton chevron suivant `>` (désactivé si mois futur)
  - Bouton rapide « Aujourd'hui » si l'utilisateur navigue dans un mois passé
- **En-tête des jours de la semaine** : `Lun · Mar · Mer · Jeu · Ven · Sam · Dim`
- **Grille des jours du mois (7 colonnes)** :
  - Chaque cellule affiche le numéro du jour.
  - Pastille d'état :
    - Vert émeraude avec coche si journée confirmée à 0 cigarette.
    - Badge numérique discret (ex. `3`, `5`) si des cigarettes sont enregistrées ce jour-là.
    - Bordure en accent émeraude pour la date actuellement sélectionnée.
    - Fond subtil pour « Aujourd'hui ».
- **Section Détail du jour sélectionné (directement sous le calendrier)** :
  - Date formatée en français (ex. « Samedi 7 novembre 2026 »).
  - Statut de la journée (En cours, Confirmée complète, Non confirmée).
  - Statistiques d'espacement (Min, Moy, Max) pour cette date si $\ge 2$ cigarettes.
  - Liste détaillée des cigarettes fumées (heure, contexte, intensité, note, modification, suppression).
  - Actions rapides pour ce jour : « Ajouter une cigarette rétroactive », « Confirmer la journée complète ».

---

## 3. Décisions Produit & Rationale Technique

1. **Calcul pur en mémoire basé sur le snapshot existant** :
   - `CapZeroUiState` contient déjà la liste complète des cigarettes (`uiState.cigarettes`), les statuts journaliers (`uiState.dailyStatuses`), les plafonds planifiés et les statistiques d'intervalles calculées par jour.
   - Le calendrier n'a besoin d'aucune requête supplémentaire en base de données : il se base sur `ymdToLocalEpochDay` et la map des jours existante pour une réactivité instantanée à 60 FPS sans latence.
2. **Gestion du fuseau horaire de l'utilisateur** :
   - Utilisation des utilitaires `DateAndCurrencyUtils` respectant le fuseau horaire local (`Africa/Tunis` ou heure de l'appareil) pour l'alignement exact des jours du mois et du premier jour de la semaine (lundi).
3. **Maintien de la vue Liste** :
   - Les utilisateurs aimant faire défiler l'historique complet sans cliquer sur chaque date conservent leur expérience habituelle grâce au commutateur `Calendrier / Liste`.

---

## 4. Architecture Technique & Flux de Données

```
┌──────────────────────────────────────────────────────────────┐
│                      JournalScreen                           │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Bannière Haut : Total Cumulé de Cigarettes Fumées     │  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Sélecteur de Mode : [ 📅 Calendrier ]  [ 📋 Liste ]    │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  SI MODE CALENDRIER :                                        │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ MonthNavigator (< Novembre 2026 > [Aujourd'hui])      │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │ DaysOfWeekHeader (Lun Mar Mer Jeu Ven Sam Dim)         │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │ MonthDaysGrid (Cases interactives avec pastilles)     │  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ SelectedDateDetailCard (Directement sous le calendrier)│  │
│  │ • Titre date & statut confirmation                    │  │
│  │ • Espacement intra-journée (Min / Moy / Max)           │  │
│  │ • Liste des cigarettes (heure, contexte, edit/delete) │  │
│  │ • Bouton ajouter / confirmer la journée               │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  SI MODE LISTE :                                             │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Liste chronologique des groupes par jour (existante)   │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### Plan de Déploiement :
1. **Utilitaires calendrier dans `DateAndCurrencyUtils.kt`** :
   - Fonctions pures pour récupérer le nombre de jours d'un mois, le décalage du 1er jour (lundi = 0), le nom du mois en français, et la navigation mois précédent / mois suivant.
2. **Composables Calendrier dans `JournalScreen.kt`** :
   - Carte de résumé du total de cigarettes fumées en haut du Journal.
   - Commutateur `Calendrier` / `Liste`.
   - Composant `JournalMonthCalendar` avec sélection réactive de la date.
   - Composant `SelectedDayHistoryView` affichant l'historique, les intervalles et les actions pour la date choisie.
3. **Ajout du total cumulé dans `ProgressScreen.kt`** :
   - Carte de statistique mettant en évidence le volume global cumulé (`uiState.progressMetrics.totalCigarettesAllDays`).
4. **Vérification & Tests** :
   - Tests unitaires Robolectric validant le calcul mensuel, la sélection de date et le total de cigarettes.
   - Compilation complète `compile_applet`.
