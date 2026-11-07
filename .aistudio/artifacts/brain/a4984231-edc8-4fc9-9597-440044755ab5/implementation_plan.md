# Personnalisation de la liste des contextes dans Cap Zéro

Ce plan détaille l'ajout d'une gestion complète et personnalisable de la liste des contextes (« Contexte (facultatif) »), permettant d'ajouter, modifier ou supprimer librement tous les contextes à la fois lors de la saisie d'une cigarette et depuis l'écran **Paramètres**.

## User Review & Critical Decisions

> [!IMPORTANT]
> Les choix suivants ont été confirmés avec vous et guident cette implémentation :

- **Personnalisation complète de la liste** : Vous pouvez **ajouter, modifier (renommer) ou supprimer librement tous les contextes** (y compris les 5 contextes proposés par défaut), avec un bouton permettant de restaurer la liste par défaut si besoin.
- **Double accès (Saisie & Paramètres)** :
  1. **Directement dans la fenêtre de saisie de cigarette** (`CigaretteEntryDialog` sur **Accueil** et **Journal**) : ajout rapide d'un nouveau contexte (qui est automatiquement sélectionné et enregistré dans votre liste) et mode d'édition rapide pour renommer ou supprimer des contextes existants.
  2. **Dans l'onglet Paramètres** : une carte dédiée **« Liste des contextes (Journal) »** permettant d'ajouter, renommer, supprimer ou réinitialiser vos contextes à tout moment.

---

## 1. Overview & Core Concept

- **Ce que fait la fonctionnalité** : Remplace la liste figée des 5 contextes prédéfinis par une liste personnelle dynamique persistée localement et incluse dans les sauvegardes JSON.
- **Valeur ajoutée** : Chaque fumeur a ses propres déclencheurs et habitudes (ex. *« En voiture »*, *« Après le sport »*, *« Appel téléphonique »*, *« Soirée jeux »*). Pouvoir créer et ajuster sa propre liste rend le suivi et les statistiques des contextes fréquents dans l'onglet **Progrès** beaucoup plus pertinents.

---

## 2. User Experience & Visual Design

### Parcours 1 : Ajout et gestion directe lors de la saisie d'une cigarette
1. Dans la fenêtre d'enregistrement ou de modification d'une cigarette (**Accueil** après *« J'ai fumé »* ou **Journal** via *« Ajouter une cigarette »* / *« Modifier »*), la section **« Contexte (facultatif) »** affiche vos puces (`FilterChip`) personnalisées.
2. Une puce d'action **« + Ajouter »** située à la fin de la liste permet de saisir immédiatement un nouveau contexte :
   - Dès la validation, le nouveau contexte est ajouté à votre liste personnelle et sélectionné automatiquement pour la cigarette en cours.
3. Un bouton **« Gérer »** en haut à droite de la section « Contexte (facultatif) » permet d'afficher les contrôles d'édition rapide (renommer ou supprimer un contexte existant) sans quitter la fenêtre de saisie.

### Parcours 2 : Gestion complète dans l'écran Paramètres
1. Dans **Paramètres**, une nouvelle carte blanche **« Liste des contextes (Journal) »** affiche l'ensemble de vos contextes actuels.
2. Un champ de saisie avec bouton **« Ajouter »** permet d'insérer rapidement un nouveau contexte dans votre liste.
3. Chaque contexte dispose d'une action **Modifier (renommer)** et **Supprimer**.
4. Un bouton secondaire **« Réinitialiser par défaut »** permet de retrouver les 5 contextes initiaux si souhaité.

### Identité visuelle & Thème
- Respect strict de la palette **Cap Zéro** :
  - Puces sélectionnées en vert menthe doux (`#D9F2E6`) avec texte vert profond (`#123D32`).
  - Cartes blanches (`#FFFFFF`) avec bordure subtile (`#E2ECE8`) et angles arrondis (`20.dp`).
  - Cibles tactiles accessibles (`≥ 48.dp`).

---

## 3. Key Product Decisions & Trade-Offs

- **Décision 1 : Synchronisation des cigarettes existantes lors du renommage d'un contexte**
  - *Approche choisie* : Lorsqu'un contexte est renommé, le nouveau libellé remplace l'ancien dans la liste personnalisée et met également à jour les entrées du journal qui utilisaient l'ancien libellé.
  - *Pourquoi* : Évite de fragmenter les statistiques de l'onglet **Progrès** (« Contextes fréquents ») entre l'ancien et le nouveau nom d'un même contexte.
- **Décision 2 : Conservation des contextes historiques lors d'une suppression**
  - *Approche choisie* : Supprimer un contexte le retire de la liste de sélection pour les prochaines saisies, sans effacer le contexte déjà enregistré sur d'anciennes cigarettes. Si vous éditez une ancienne cigarette ayant un contexte supprimé, celui-ci reste visible et sélectionnable sur cette fiche.
  - *Pourquoi* : Garantit qu'aucune donnée historique du journal n'est perdue.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Diagramme des composants

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Couche de Persistance Locale                         │
│  ┌──────────────────────────────────┐  ┌─────────────────────────────┐  │
│  │ DataStore (Preferences)          │  │ Room Database               │  │
│  │ • customContexts (JSON Array)    │  │ • CigaretteLogEntity        │  │
│  │   (défaut : PresetContexts.ALL)  │  │   (contextCategory: String) │  │
│  └───────────────┬──────────────────┘  └──────────────┬──────────────┘  │
└──────────────────┼────────────────────────────────────┼─────────────────┘
                   │                                    │
                   ▼                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       CapZeroRepository                                 │
│  • addCustomContext(label) / updateCustomContext(old, new)              │
│  • removeCustomContext(label) / resetCustomContextsToDefault()          │
│  • Export / Import JSON incluant customContexts                         │
└──────────────────────────────────┬──────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                  CapZeroViewModel (StateFlow<CapZeroUiState>)           │
│  • Expose uiState.preferences.customContexts                            │
│  • Actions : addCustomContext, renameCustomContext,                     │
│              deleteCustomContext, resetCustomContexts                   │
└─────────────────┬────────────────────────────────────┬──────────────────┘
                  │                                    │
                  ▼                                    ▼
┌────────────────────────────────────┐  ┌─────────────────────────────────┐
│ CigaretteEntryDialog               │  │ SettingsScreen                  │
│ (Accueil & Journal)                │  │ • Carte « Liste des contextes » │
│ • Puces dynamiques                 │  │ • Champ d'ajout rapide          │
│ • Puce « + Ajouter » inline        │  │ • Renommage & suppression       │
│ • Mode « Gérer » (éditer/suppr.)   │  │ • Réinitialisation par défaut   │
└────────────────────────────────────┘  └─────────────────────────────────┘
```

### Modèle de données & État
- **`UserPreferencesState`** :
  - Ajout de `val customContexts: List<String> = PresetContexts.ALL`.
  - Sérialisation JSON dans DataStore via la clé `custom_contexts_json` (et sauvegarde/restauration dans l'export JSON complet).
- **Opérations dans le Repository & DAO** :
  - Ajout d'une requête DAO `renameContextCategory(oldCategory: String, newCategory: String)` pour maintenir la cohérence des statistiques lorsqu'un contexte est renommé.
  - Validation des libellés (suppression des espaces superflus, prévention des doublons insensibles à la casse).
