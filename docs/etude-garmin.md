# Étude préalable de l'API Garmin Activity

Étude réalisée le 21 août 2026 exclusivement à partir des pages publiques officielles Garmin.

## Conclusion

L'API Activity officielle fournit les activités détaillées et les fichiers FIT après synchronisation de la montre avec Garmin Connect. Elle propose les architectures Push et Ping/Pull, des flux sélectionnables, le backfill et des outils d'auto-vérification. En revanche, le programme Garmin Connect Developer est annoncé comme réservé à un usage professionnel. Une demande et une approbation Garmin sont obligatoires avant d'obtenir l'environnement d'évaluation et la documentation technique complète.

À ce stade, il est donc impossible de déclarer l'intégration opérationnelle ou de fixer sans spéculation les URL, charges utiles, limites de débit et événements exacts.

## Accès et autorisation

- La demande d'accès est examinée par Garmin, qui annonce une réponse sur son statut sous deux jours ouvrés. Garmin indique qu'une intégration prend généralement une à quatre semaines.
- Après approbation, Garmin ouvre le portail développeur et un environnement d'évaluation. La page Activity mentionne des données d'exemple, le backfill et une auto-vérification avant production.
- Les API du programme utilisent OAuth 2.0. Il faut donc prévoir au minimum un identifiant client, un secret client et une URL de redirection enregistrée, sans inventer leur format tant que le portail n'est pas accessible.
- L'utilisateur doit donner son consentement initial. Ses activités deviennent ensuite accessibles après synchronisation du dispositif vers Garmin Connect.
- La révocation fait partie du contrat à prévoir, mais son endpoint et ses notifications exactes ne sont pas exposés dans les pages publiques consultées.

## Synchronisation et données

- Deux architectures sont officiellement proposées : Push et Ping/Pull.
- L'API donne accès aux détails complets sous forme de fichiers FIT, GPX ou TCX, dont le FIT pour les données détaillées.
- Les flux peuvent être limités aux données nécessaires.
- Le backfill des données utilisateur est explicitement annoncé dans les outils développeur.
- Les métriques réellement disponibles varient selon l'appareil et le type d'activité. Le backend ne devra jamais inventer une métrique absente.

## Hébergement personnel sur NAS

Garmin décrit une intégration cloud-à-cloud et le programme comme destiné aux entreprises. Une application strictement personnelle n'est donc pas garantie éligible. Push, Ping et OAuth exigent vraisemblablement des URL de rappel atteignables par Garmin en HTTPS. Une exposition HTTPS minimale derrière un mandataire inverse serait compatible avec le NAS sur le plan technique, mais les exigences précises sur les URL ne sont accessibles qu'après approbation.

## Sécurité, attribution et limites

- Les secrets OAuth ne doivent être ni intégrés aux images, ni écrits dans les journaux. Les jetons doivent être chiffrés au repos.
- Les vues présentant des données issues d'un appareil Garmin doivent respecter les règles d'attribution Garmin. Les éléments de marque officiels ne doivent pas être altérés.
- Les limites de débit, durées de conservation, procédures détaillées de révocation et obligations contractuelles ne sont pas publiées sur les pages publiques étudiées. Elles devront être relevées dans le contrat et le portail après approbation, avant toute mise en production.

## Possibilités de test

Avant approbation, seuls le domaine, les adaptateurs factices et les fixtures anonymisées peuvent être testés localement. Après approbation, Garmin annonce un environnement d'évaluation, des données d'exemple et des outils d'auto-vérification. Aucun test automatisé ne devra appeler le compte Garmin réel.

## Décision nécessaire

La voie sûre reste l'API officielle, sous réserve que Garmin accepte ce projet personnel. En cas de refus, un connecteur non officiel demanderait une autorisation explicite distincte après étude de ses risques; il ne doit pas être introduit silencieusement.

## Sources officielles

- [Garmin Activity API](https://developer.garmin.com/gc-developer-program/activity-api/)
- [Présentation du Garmin Connect Developer Program](https://developer.garmin.com/gc-developer-program/overview/)
- [FAQ du Garmin Connect Developer Program](https://developer.garmin.com/gc-developer-program/program-faq/)
- [Règles de marque des API Garmin](https://developer.garmin.com/downloads/brand/Garmin-Developer-API-Brand-Guidelines.pdf)
