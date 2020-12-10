# SAP Commerce Project Template for CCv2

1. Download this repository
1. Download the latest SAP Commerce 2011 release zip file and put it into the `platform` folder using the correct file name, e.g.
   
   ```bash
   cp ~/Downloads/CXCOMM201100P*.ZIP ./platform/hybris-commerce-suite-2011.0.zip
   ```

1. Bootstrap the starting point for your Commerce project by running the following command:
   
   ```bash
   ./gradlew -b bootstrap.gradle.kts \
     -PprojectName=<name, e.g. coolshop> \
     -ProotPackage=<package, e.g. com.cool.shop>
   ```

   (N.B.: If you use a headless setup: You can delete the generated `<something>storefront` extension afterwards. Don't forget to remove it from `localextensions.xml` / `manifest.json`)
1. Review the generated configuration in `hybris/config`, especially the `hybris/config/environment/*.properties` files and `localextensions.xml` (search for `TODO:` comments)
1. Update the `manifest.jsonnet` (again, search for `TODO:` comments).\
   You can use the jsonnet file to update the `manifest.json` for your project.
1. Delete all bootstrap files, you don't need them anymore:

   ```bash
   rm -r bootstrap*
   ```

1. Commit and push the changes to your project repository :) 

After the initial setup is done, you can use all the cool features of the `commerce-gradle-plugin`.

## Setup local development environment after a fresh clone

```sh
git clone <project>
cd <project>
docker-compose up
cd core-customize
./gradlew setupLocalDevelopment
./gradlew yclean yall
./gradlew yinitialize
```

## How to use manifest.jsonnet

To generate the `manifest.json` with [Jsonnet](https://jsonnet.org/):

```bash
jsonnet --output-file manifest.json manifest.jsonnet
```

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

## FAQ

### How do I add an addon to my storefront?

1. Add the addon to the `manifest.json`, either by hand or via `manifest-generator.jsonnet` ([documentation][addon])
1. Run `./gradlew installManifestAddon`
1. Reformat `<storefront>/extensioninfo.xml` (unfortunately, the the platform build messes it up when adding addons)
1. Commit/push your changes

[addon]: https://help.sap.com/viewer/1be46286b36a4aa48205be5a96240672/LATEST/en-US/9a3ab7d08c704fccb7fd899e876d41d6.html

## Demo Setup

1. Generate the sample `manifest.json` as described above
1. Download the latest Commerce platform 2011 zip file and save it as `platform/hybris-commerce-suite-2011.0.zip`
1. Generate `demoshop` storefront and `hybris/config` folder: `./gradlew generateProprietaryCode`
1. Setup local development `./gradlew setupLocalDevelopment`
