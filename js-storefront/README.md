# JS Storefront Template

To bootstrap a new Spartacus storefront, run:

```bash
./boostrap.sh <project name>
```

You can delete the `bootstrap` folder and script afterwards.

## Requirements

- [Angular CLI](https://angular.io/): **12.0** or later.
- node.js: 14.x [1]
- yarn: v1.15 or later

(ref [Building the Spartacus Storefront Using 4.x Libraries][libraries])

[1]: Node.js 12 is [EOL by 2022-04-30](https://nodejs.org/en/about/releases/)

[libraries]: https://sap.github.io/spartacus-docs/building-the-spartacus-storefront-from-libraries-4-x/#front-end-development-requirements

## What does the script do?

- Bootstraps a new Spartacus project from scratch as recommended by the official documentation (with PWA and SSR support)
- Enable optimizations (patches in `bootstrap/patches`):
  - Configuration for "SAP Commerce in the Public Cloud" (aka CCv2) (including [Smartedit setup][smartedit])
  - Minor tweaks to `package.json`:
    - Run production build on `yarn build` (see [Updating the Code Repository for JavaScript Storefronts][build])
    - Use SSL for local development server (`yarn start`)
    - Disable certificate checks for SSR development server (makes connection to local SAP Commerce development server possible)
  - [Recommended developer settings][developer] for [VS Code][code]
- Generate a `manifest.json` for CCv2 with correct settings for [Client Side][csr] and [Server Side Rendering][ssr]

[developer]: https://sap.github.io/cloud-commerce-spartacus-storefront-docs/recommended-development-environment/
[code]: https://code.visualstudio.com/
[build]: https://help.sap.com/viewer/b2f400d4c0414461a4bb7e115dccd779/LATEST/en-US/63577f67a67347bf9f4765a5385ead33.html
[smartedit]: https://sap.github.io/cloud-commerce-spartacus-storefront-docs/smartEdit-setup-instructions-for-spartacus/
[issue]: https://github.com/SAP/spartacus/issues/5886

[ssr]: https://help.sap.com/viewer/b2f400d4c0414461a4bb7e115dccd779/LATEST/en-US/cd5b94c25a68456ba5840f942f33f68b.html
[csr]: https://help.sap.com/viewer/b2f400d4c0414461a4bb7e115dccd779/LATEST/en-US/0d54fc7faaa44b14b15b164cb1f3f2b6.html
