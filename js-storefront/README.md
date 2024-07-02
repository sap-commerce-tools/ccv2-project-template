# JS Storefront Template

## Requirements

See [Recommended Development Environment][requirements].

- [Angular CLI](https://angular.io/): **17.0** or later.
- node.js: 20.9.0 or later
- yarn

[requirements]: https://help.sap.com/docs/SAP_COMMERCE_COMPOSABLE_STOREFRONT/cfcf687ce2544bba9799aa6c8314ecd0/bf31098d779f4bdebb7a2d0591917363.html?locale=en-US

## Getting started

To bootstrap a new Spartacus storefront:

1. Get credentials at the [Repository based shipment channel][rbsc]
2. Export your base64 NPM credentials as Environment variable `RBSC_NPM_CREDENTIALS`.
3. Run the bootstrap script with your repo name
   ```bash
   ./boostrap.sh <project name>
   ```
4. You can delete the `bootstrap` folder and script afterwards.

[rbsc]: https://ui.repositories.cloud.sap/www/webapp/users/

## What does the script do?

- Bootstraps a new Spartacus project from scratch as recommended by the official documentation (with PWA and SSR support)
- Generate a `manifest.json` for CCv2 with correct settings for [Client Side][csr] and [Server Side Rendering][ssr]
- Generate a `.npmrc` for configuration as described in [Installing Composable Storefront Libraries from the Repository
  Based Shipment Channel][library] with a variable placeholder.

[library]: https://help.sap.com/docs/SAP_COMMERCE_COMPOSABLE_STOREFRONT/cfcf687ce2544bba9799aa6c8314ecd0/5de67850bd8d487181fef9c9ba59a31d.html?locale=en-US#installing-composable%0Astorefront-libraries-from-the-repository-based-shipment-channel
[build]: https://help.sap.com/viewer/b2f400d4c0414461a4bb7e115dccd779/LATEST/en-US/63577f67a67347bf9f4765a5385ead33.html
[issue]: https://github.com/SAP/spartacus/issues/5886

[ssr]: https://help.sap.com/viewer/b2f400d4c0414461a4bb7e115dccd779/LATEST/en-US/cd5b94c25a68456ba5840f942f33f68b.html
[csr]: https://help.sap.com/viewer/b2f400d4c0414461a4bb7e115dccd779/LATEST/en-US/0d54fc7faaa44b14b15b164cb1f3f2b6.html
