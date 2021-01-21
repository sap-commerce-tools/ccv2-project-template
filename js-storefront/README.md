# JS Storefront Template

To bootstrap a new Spartacus storefront, run:

```
./boostrap.sh <project name>
```

You can delete the `bootstrap` folder and script afterwards.

## Requirements

- Node.js 12.x
- Yarn 1.15+
- Angular CLI (`ng`) 10.1.x

(ref [Building the Spartacus Storefront Using 3.x Libraries][libraries])

[libraries]: https://sap.github.io/spartacus-docs/building-the-spartacus-storefront-from-libraries/#front-end-development-requirements

## What does the script do?

- Bootstraps a new Spartacus project from scratch as recommended by the official documentation
- Generate a `manifest.json` for commerce cloud
- Enable optimizations:
  - Configuration for "SAP Commerce in the Public Cloud" (aka CCv2) (including the [Smartedit setup][smartedit])
  - [Recommended developer settings][developer] for [VS Code][code]
  - Streamlined OCC settings (check [`src/environments`](src/environments) and [`app.module.ts`](src/app/app.module.ts#L10-L17)) and [SAP/spartacus#5886][issue]
  - Minor tweaks to `package.json`:
    - Run production build on `yarn build` (see [Updating the Code Repository for JavaScript Storefronts][build])
    - Use SSL for local development server (`yarn start`)

[spartacus]: https://github.com/SAP/cloud-commerce-spartacus-storefront
[developer]: https://sap.github.io/cloud-commerce-spartacus-storefront-docs/recommended-development-environment/
[code]: https://code.visualstudio.com/
[build]: https://help.sap.com/viewer/b2f400d4c0414461a4bb7e115dccd779/LATEST/en-US/63577f67a67347bf9f4765a5385ead33.html
[smartedit]: https://sap.github.io/cloud-commerce-spartacus-storefront-docs/smartEdit-setup-instructions-for-spartacus/
[ssr]: https://sap.github.io/spartacus-docs/ssr-ccv2-issue-spartacus-version-2/
[issue]: https://github.com/SAP/spartacus/issues/5886
