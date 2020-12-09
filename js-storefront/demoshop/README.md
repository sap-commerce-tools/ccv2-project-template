# Demoshop

A [Spartacus][spartacus]-based demo storefront ([built from libraries][libraries]) with:

- Configuration for "SAP Commerce in the Public Cloud" (aka CCv2) (including the [Smartedit setup][smartedit])
- [Recommended developer settings][developer] for [VS Code][code]
- Streamlined OCC settings (check [`src/environments`](src/environments) and [`app.module.ts`](src/app/app.module.ts#L11-L16)) and SAP/spartacus/issues#5886
- Minor tweaks to `package.json`:
  - run production build on `yarn build` (see [Updating the Code Repository for JavaScript Storefronts][build])
- Includes the [SSR workaround][ssr]

[spartacus]: https://github.com/SAP/cloud-commerce-spartacus-storefront
[libraries]: https://sap.github.io/cloud-commerce-spartacus-storefront-docs/building-the-spartacus-storefront-from-libraries/
[developer]: https://sap.github.io/cloud-commerce-spartacus-storefront-docs/recommended-development-environment/
[code]: https://code.visualstudio.com/
[build]: https://help.sap.com/viewer/1be46286b36a4aa48205be5a96240672/SHIP/en-US/63577f67a67347bf9f4765a5385ead33.html
[smartedit]: https://sap.github.io/cloud-commerce-spartacus-storefront-docs/smartEdit-setup-instructions-for-spartacus/
[ssr]: https://sap.github.io/spartacus-docs/ssr-ccv2-issue-spartacus-version-2/

## Notes

- You have to copy the `webApplicationInjector.js` from SAP Commerce and add it to the project (I want to avoid any copyright troubles). Assuming the default project layout for CCv2, this should do the trick:

  ```bash
  cp ../../core-customize/hybris/bin/modules/smartedit/smarteditaddon/acceleratoraddon/web/webroot/_ui/shared/common/js/webApplicationInjector.js src/assets
  ```

- \[optional\] Restrict Smartedit to your CCv2 subscription by changing the value of `data-smartedit-allow-origin` in [`src/index.html`](src/index.html#L13). \
  See [Whitelisting SmartEdit for Your Storefront][whitelisting] for details.
- The configuration assumes that you use the [Spartacus Sample Data Addon][sample] in SAP Commerce
  > BTW, this is OK for demos, but **never** start a real project with any of the sample data extensions!

[whitelisting]: https://help.sap.com/viewer/86dd1373053a4c2da8f9885cc9fbe55d/latest/en-US/fb742b29cf3c4e81aac7c131c0441172.html
[sample]: https://sap.github.io/cloud-commerce-spartacus-storefront-docs/installing-sap-commerce-cloud/#installing-the-spartacus-sample-data-addon
