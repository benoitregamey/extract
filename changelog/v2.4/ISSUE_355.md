# ISSUE_355 - Bouton de navigation manquant sur la page des groupes

## Status: ✅ CONFORME

### Issue Description
Sur la page de gestion des groupes (`/userGroups`), il manquait un bouton « Utilisateurs »
permettant de revenir à la page « Utilisateurs et droits ». La page utilisateurs dispose
déjà d'un bouton « Groupes » symétrique ; la navigation n'était possible que dans un sens
sans repasser par le menu.

### Acceptance Criteria
| Identifiant | Description |
| --- | --- |
| 355-1 | Le nouveau bouton est implémenté comme dans la maquette et est fonctionnel (allers/retours possibles entre groupes et utilisateurs sans passer par le menu) |

### Implementation Completed
1. Ajout d'un bouton « Utilisateurs » dans `templates/pages/userGroups/list.html`, pointant
   vers `/users`, calqué sur le bouton « Groupes » existant de la page utilisateurs
   (style `btn-extract-white`, icône `fa-user`).
2. Externalisation du libellé via la nouvelle clé i18n `userGroupsList.users.button`,
   disponible en **français** (Utilisateurs), **allemand** (Benutzer) et anglais (Users).

### Tests
- `UserGroupManagementIntegrationTest` : nouveau test `5.1b` vérifiant que la page
  `/userGroups` rend bien le bouton de navigation vers `/users`.

### Conclusion
Le critère 355-1 est satisfait : la navigation aller/retour entre groupes et utilisateurs
est possible sans passer par le menu.
