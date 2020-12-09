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
1. Download the latest Commerce platform 2011 zip file and save it as `platform/hybris-commerce-suite-2011.0.zip`
1. Generate `demoshop` storefront and `hybris/config` folder: `./gradlew generateProprietaryCode`
1. Setup local development `./gradlew setupLocalDevelopment`

## How does it work?

We use Gradle + [commerce-gradle-plugin][plugin] to automate whole project setup.

[plugin]: https://github.com/SAP/commerce-gradle-plugin

By combining the [configuration reuse][reuse] mechanism of CCv2, the [optional configuration folder][folder] of Commerce and a bit of clever symlinking of files and folders, we can use the same configuration locally and in the cloud.

This setup uses:

- `hybris/config/localextensions.xml` to configure extensions
- `hybris/config/environments/*.properties` to configure properties per CCv2 aspect.
   There is one file per aspect, plus the special file `local-dev.properties` that configures the local development environment
- `hybris/config/local-config` is configured as `hybris.optional.config.dir` and contains *symlinks* 
  to the relevant property files in `hybris/config/environments` (by default: `common.properties` and `local-dev.properties`).\
  **Important** Contrary to more default setups, `local.properties` must not be modified at all (that's why it is in `.gitignore`).
  - If you have any configuration specific to your local machine, put it in `hybris/config/local-config/99-local.properties`.
  - If the local setup changes for the whole project, update `hybris/config/environments/local-dev.properties`
- The default cloud solr configuration set is contained in the correct folder structure for CCv2 ([documentation][solr]).
  A symlink in `hybris/config/solr` allows to use the same configuration locally.

```
                                  core-customize
                                  ├── ...
                                  ├── hybris
                                  ├── ...
                                  │  ├── config
                                  │  │  ├── environments
                        +--------------------> accstorefront.properties
                        |--------------------> admin.properties
                        |--------------------> api.properties
                        |--------------------> backgroundprocessing.properties
                        |--------------------> backoffice.properties
                        +--------------------> common.properties     <---+
                        |         │  │  │  └── local-dev.properties <--+ |
                        |         │  │  ├── ...                        | | symlinks
                        |         │  │  ├── local-config               | |
manifest.json           |         │  │  │  ├── 10-local.properties +-----+
  useConfig             |         │  │  │  ├── 50-local.properties +---+
    properties          |         │  │  │  └── 99-local.properties
      ... +-------------+         │  │  ├── local.properties
    extensions +--------------------------> localextensions.xml
    solr +---------+              │  │  ├── readme.txt
                   |              │  │  ├── solr
                   |              │  │  │  └── instances
                   |              │  │  │     └── cloud
                   |              │  │  │        ├── configsets +-------+
                   |              │  │  │        ├── ...                |
                   |              │  │  │        └── zoo.cfg            |
                   |              │  │  └── ...                         |
                   |              ├── ...                               |  symlink
                   +----------------> solr                              |
                                  │  └── server                         |
                                  │     └── solr                        |
                                  │        └── configsets <-------------+
                                  │           └── default
                                  │              └── conf
                                  │                 ├── lang
                                  │                 ├── protwords.txt
                                  │                 ├── schema.xml
                                  │                 ├── solrconfig.xml
                                  │                 ├── stopwords.txt
                                  │                 └── synonyms.txt
                                  └── ...

```


[reuse]: https://help.sap.com/viewer/1be46286b36a4aa48205be5a96240672/LATEST/en-US/2311d89eef9344fc81ef168ac9668307.html
[folder]: https://help.sap.com/viewer/b490bb4e85bc42a7aa09d513d0bcb18e/LATEST/en-US/8beb75da86691014a0229cf991cb67e4.html
[solr]: https://help.sap.com/viewer/b2f400d4c0414461a4bb7e115dccd779/LATEST/en-US/f7251d5a1d6848489b1ce7ba46300fe6.html
