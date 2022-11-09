# CCv2 Template Repository

- Best-practice setup
- Based on SAP Commerce 2011
- Generates B2C accelerator modules, OCC extension and OCC tests extension
- Includes Spartacus storefront
- Fully automated setup for local development
- [docker-compose](https://docs.docker.com/compose/)-based setup for:
  - MS SQL
  - [Azurite](https://github.com/Azure/Azurite) (for cloud hotfolder)
  - DB snapshot import with [sqlpackage](https://learn.microsoft.com/es-es/sql/tools/sqlpackage/sqlpackage). Place your backup.bacpac file in `docker-resources` folder (for bacpac imports from CCV2 to local environment)

## How To

- Clone/Download/Fork this repository
- Follow the steps in the READMEs linked below

## SAP Commerce Setup

[core-customize/README.md](core-customize/README.md)

## Spartacus Setup

[js-storefront/README.md](js-storefront/README.md)
