# Environment Ribbons for SAP Commerce
[![ko-fi](https://www.ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/W7W7VS24)

![Ribbons](../assets/ribbons.gif?raw=true)

## Install

1. Download this repo as zip
1. Unpack the zip to your `hybris/bin/custom` folder
1. Add the `envribbon` extension to your `localextensions.xml`
1. Build

If you want to add a ribbon component to your storefront:

1. Add `envribbonaddon` to your `localextensions.xml`
1. `ant addoninstall -Daddonnames="envribbonaddon" -DaddonStorefront.yacceleratorstorefront="<your storefront>"`
1. Build
1. Update system
1. Add a `EnvRibbonComponent` to your `FooterSlot` \
   (check the demo data included in the addon for details)

## Notes

- You can "click through" the ribbon (CSS `pointer-events: none`)
- There is a *single CSS* file that controls the style of the ribbon *everywhere*: \
  [`envribbon/resources/envribbon/envribbon.css`](envribbon/resources/envribbon/envribbon.css) \
  (Build callbacks take care of copying the stylesheet where it is needed)
- If deployed in a CCv2 environment, the extension will **auto-detect** the environment (code and type)
- For on-prem environments (or if you want to override the detected values), you can use:

  ```properties
  ribbon.environment.code=<environment identifier>
  ribbon.environment.aspect=<aspect name, optional>
  ribbon.environment.type=<type>
  # development, staging, production
  ```
