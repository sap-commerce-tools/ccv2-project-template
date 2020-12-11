# SAP Commerce Project Template for CCv2

To generate the `manifest.json` with [Jsonnet](https://jsonnet.org/):

```bash
jsonnet --output-file manifest.json manifest-generator.jsonnet
```

**Alternatively**, if you don't want to install Jsonnet, run

```bash
./gradlew -b generate-manifest.gradle.kts
```

## Demo Setup

1. Generate the sample `manifest.json` as described above
1. Download the latest Commerce platform 1905 zip file and save it as `platform/hybris-commerce-suite-1905.6.zip`
1. Download the latest cloud extension pack 1905 zip file and save it as `platform/hybris-cloud-extension-pack-1905.5.zip`
1. Generate `demoshop` storefront and `hybris/config` folder: `./gradlew generateProprietaryCode`
1. Setup local development `./gradlew setupLocalDevelopment`
