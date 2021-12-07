# CCv2 Template Repository

- Best-practice setup
- Based on SAP Commerce 2011
- Generates B2C accelerator modules, OCC extension and OCC tests extension
- Includes Spartacus storefront
- Fully automated setup for local development
- [docker-compose](https://docs.docker.com/compose/)-based setup for:
  - MS SQL
  - [Azurite](https://github.com/Azure/Azurite) (for cloud hotfolder)

## How To

- Clone/Download/Fork this repository
- Follow the steps in the READMEs linked below

## SAP Commerce Setup

[core-customize/README.md](core-customize/README.md)

## Spartacus Setup

[js-storefront/README.md](js-storefront/README.md)

## Finalize Build Process

- Update build.gradle.kts file to point to the correct paths.
- Depending on the steps before, you might only need to redeclare the projectName variable.
- If commerce backend and js-storefront use different names, you need to modify the paths accordingly.
