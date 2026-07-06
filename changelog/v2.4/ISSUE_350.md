# ISSUE_350 - Retirer le paramètre paramInputData=SourceDataset_FILEGDB

## Status: ✅ CONFORME

### Issue Description
Le paramètre `paramInputData=SourceDataset_FILEGDB` du fichier
`extract-task-fmedesktop/src/main/resources/plugins/fme/properties/configFME.properties`
était inutilisé et devait être retiré.

### Conformity Analysis
**CONFORME** - Vérification effectuée : la propriété `paramInputData` n'est consommée par
aucun code de l'application. Elle n'était référencée que par le fichier `.properties`
lui-même et par un test unitaire qui en assertait la valeur.

### Implementation Completed
1. Suppression de la ligne `paramInputData=SourceDataset_FILEGDB` dans `configFME.properties`.
2. Suppression du test devenu obsolète `returnsParamInputDataProperty`
   (`PluginConfigurationTest`).

### Tests
Aucun nouveau test : il s'agit d'un retrait de configuration inutilisée. Le test couvrant
la propriété supprimée a été retiré ; le reste de `PluginConfigurationTest` continue de
couvrir les propriétés effectivement utilisées.

### Impact documentation / i18n
Aucun : la propriété n'apparaît pas dans la documentation d'architecture et ne correspond
à aucun libellé multilingue.

### Conclusion
Le paramètre inutilisé est retiré sans impact sur le comportement de l'application.
