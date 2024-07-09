import org.apache.tools.ant.taskdefs.condition.Os

import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify

import java.time.Instant

plugins {
    id("sap.commerce.build") version("4.0.0")
    id("sap.commerce.build.ccv2") version("4.0.0")
    id("de.undercouch.download") version("5.5.0") // for downloading solr-9.x.tgz and optionally SAP Commerce Cloud zips.
    `maven-publish` // for publishing to the local maven repo
}

val solrVersionMap = mapOf( //
    /*
     * version 8 unfortunately has a different download URL and is currently not working. But it is the version that is
     * supplied with the standard zip, so we're fine unless the solr version is changed and then changed back in
     * manifest.json
     */
    "8.11" to "8.11.2", //
    "9.2" to "9.2.1", //
    "9.5" to "9.5.0"
)
val solrVersion = solrVersionMap[CCV2.manifest.solrVersion] ?: "9.2.1"

val cloudHotfolderVersion = "2211"

val dependencyDir = "dependencies"
val workingDir = project.projectDir
val binDir = "${workingDir}/hybris/bin"

repositories {
    flatDir { dirs(dependencyDir) }
    mavenCentral()
}

val cloudHotfolderExtensions: Configuration by configurations.creating
val solrServer: Configuration by configurations.creating

dependencies {
    cloudHotfolderExtensions("de.hybris.platform:cloudhotfolders:${cloudHotfolderVersion}@zip")
    solrServer("org.apache.solr:solr:${solrVersion}")
}


hybris {
    //Optional mapping of preview version to patch level.
    // manifest.json requires the Commerce Suite version in the format 2211.8, while the platform build.number
    // uses 2211.FP1. This mapping allows the plugin to convert between the two formats.
    previewToPatchLevel = mapOf<String, Int>(
            "2211.FP0" to 4,
            "2211.FP1" to 8
    )

    // what files should be deleted when cleaning up the platform?
    // (cloudhofolders will be downloaded by custom configuration)
    cleanGlob.set("glob:**hybris/bin/{modules**,platform**,cloudhotfolders**}")

    // what should be unpacked from the platform zip files?
    bootstrapInclude.set(
        listOf(
            "hybris/**", //
            "azurecloudhotfolder/**", //
            "cloudcommons/**", //
            "cloudhotfolder/**" //
        )
    )

    // what should excluded when unpacking?
    // the default value is a npm package folder that includes UTF-8 filenames, which lead to problems on linux
    bootstrapExclude.set(
        listOf(
            "hybris/bin/ext-content/npmancillary/resources/npm/node_modules/http-server/node_modules/ecstatic/test/**"
        )
    )

    //Control the sparse platform bootstrap.
    //  When enabled, the commerce extensions are extracted from the distribution zip on a as-needed basis.
    //  Only extensions that are actually used in the project (either directly listed in the localextensions.xml or
    //  required by other extensions) are extracted.
    //  The platform itself is always extracted.
    //  When this mode is enabled, the bootstrapInclude configuration property is ignored.
    sparseBootstrap {
        // default is disabled
        // currently not working in combination with the separate cloudhotfolders zip.
        enabled = false
        // set of extensions that are always extracted, default is an empty set
        alwaysIncluded = listOf<String>()
    }
}

/*
 * Cloud hotfolders are transparently provided in commerce cloud. Since some of extensions might depend on them, we
 * also have them locally.
 */
tasks.register<Copy>("bootstrapCloudhotfolder") {
    group = "build setup"
    dependsOn("bootstrapPlatform")
    onlyIf {
        !file("${binDir}/cloudhotfolders").exists()
    }

    from(zipTree(cloudHotfolderExtensions.singleFile))
    into("${binDir}/cloudhotfolders")
}

tasks.register<Download>("fetchSolr") {
    src(uri("https://archive.apache.org/dist/solr/solr/${solrVersion}/solr-${solrVersion}.tgz"))
    dest("${dependencyDir}/solr-${solrVersion}.tgz")
    overwrite(false) // to only download solr into the dependency folder if it's not there yet.
}

val repackSolr = tasks.register<Zip>("repackSolr") {
    dependsOn("fetchSolr")
    from(tarTree("${dependencyDir}/solr-${solrVersion}.tgz"))
    archiveFileName = "solr-${solrVersion}.zip"
    destinationDirectory = file(dependencyDir)
}

publishing {
    publications {
        create<MavenPublication>("solr") {
            groupId = "org.apache.solr"
            artifactId = "solr"
            version = solrVersion

            artifact(repackSolr.get().archiveFile)
        }
    }
}

tasks.named("yinstallSolr") {
    // this task is available because of the solr-publication above.
    dependsOn("publishSolrPublicationToMavenLocal")
}

tasks.ybuild {
    dependsOn("publishSolrPublicationToMavenLocal")
    group = "build"
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "8.7"
}

//Optional: automate downloads from launchpad.support.sap.com
//  remove this block if you use something better, like Maven
//  Recommended reading: 
//  https://github.com/SAP/commerce-gradle-plugin/blob/master/docs/FAQ.md#downloadPlatform
if (project.hasProperty("sUser") && project.hasProperty("sUserPass")) {
    val sUser = project.property("sUser") as String
    val sUserPassword = project.property("sUserPass") as String
    val authorization = java.util.Base64.getEncoder().encodeToString(("$sUser:$sUserPassword").toByteArray())

    val commerceVersion = CCV2.manifest.commerceSuiteVersion
    val commerceSuiteDownloadUrl = project.property("com.sap.softwaredownloads.commerceSuite.${commerceVersion}.downloadUrl")
    val commerceSuiteChecksum = project.property("com.sap.softwaredownloads.commerceSuite.${commerceVersion}.checksum")
    
    tasks.register<Download>("downloadPlatform") {
        src(commerceSuiteDownloadUrl)
        dest(file("${dependencyDir}/hybris-commerce-suite-${commerceVersion}.zip"))
        header("Authorization", "Basic $authorization")
        overwrite(false)
        tempAndMove(true)
        onlyIfModified(true)
        useETag(true)
    }

    tasks.register<Verify>("downloadAndVerifyPlatform") {
        dependsOn("downloadPlatform") 
        src(file("${dependencyDir}/hybris-commerce-suite-${commerceVersion}.zip"))
        algorithm("SHA-256")
        checksum("$commerceSuiteChecksum")
    }

    tasks.named("bootstrapPlatform") {
        dependsOn("downloadAndVerifyPlatform")
    }

    //check if Integration Extension Pack is configured and download it too
    if (CCV2.manifest.extensionPacks.any{ "hybris-commerce-integrations" == it.name }) {
        val integrationExtensionPackVersion = CCV2.manifest.extensionPacks.first{ "hybris-commerce-integrations" == it.name }.version
        val commerceIntegrationsDownloadUrl = project.property("com.sap.softwaredownloads.commerceIntegrations.${integrationExtensionPackVersion}.downloadUrl")
        val commerceIntegrationsChecksum = project.property("com.sap.softwaredownloads.commerceIntegrations.${integrationExtensionPackVersion}.checksum")
        
        tasks.register<Download>("downloadIntExtPack") {
            src(commerceIntegrationsDownloadUrl)
            dest(file("${dependencyDir}/hybris-commerce-integrations-${integrationExtensionPackVersion}.zip"))
            header("Authorization", "Basic $authorization")
            overwrite(false)
            tempAndMove(true)
            onlyIfModified(true)
            useETag(true)
        }

        tasks.register<Verify>("downloadAndVerifyIntExtPack") {
            dependsOn("downloadIntExtPack")
            src(file("${dependencyDir}/hybris-commerce-integrations-${integrationExtensionPackVersion}.zip"))
            algorithm("SHA-256")
            checksum("$commerceIntegrationsChecksum")
        }

        tasks.named("bootstrapPlatform") {
            dependsOn("downloadAndVerifyIntExtPack")
        }
    }
}

tasks.register<WriteProperties>("generateLocalProperties") {
    comment = "GENERATED AT " + Instant.now()
    destinationFile = project.file("hybris/config/local.properties")
    property("hybris.optional.config.dir", project.file("hybris/config/local-config").absolutePath)
    doLast {
        mkdir(project.file("hybris/config/local-config/"))
    }
}

val symlinkConfigTask: TaskProvider<Task> = tasks.register("symlinkConfig")
val localConfig = file("hybris/config/local-config")
mapOf(
    "10-local.properties" to file("hybris/config/cloud/common.properties"),
    "20-local.properties" to file("hybris/config/cloud/persona/development.properties"),
    "50-local.properties" to file("hybris/config/cloud/local-dev.properties")
).forEach{
    val symlinkTask = tasks.register<Exec>("symlink${it.key}") {
        val path = it.value.relativeTo(localConfig)
        if (Os.isFamily(Os.FAMILY_UNIX)) {
            commandLine("sh", "-c", "ln -sfn $path ${it.key}")
        } else {
            // https://blogs.windows.com/windowsdeveloper/2016/12/02/symlinks-windows-10/
            val windowsPath = path.toString().replace("[/]".toRegex(), "\\")
            commandLine("cmd", "/c", """mklink "${it.key}" "$windowsPath" """)
        }
        workingDir(localConfig)
        dependsOn("generateLocalProperties")
    }
    symlinkConfigTask.configure {
        dependsOn(symlinkTask)
    }
}

tasks.register<WriteProperties>("generateLocalDeveloperProperties") {
    dependsOn(symlinkConfigTask)
    comment = "my.properties - add your own local development configuration parameters here"
    destinationFile = project.file("hybris/config/local-config/99-local.properties")
    onlyIf {
        !project.file("hybris/config/local-config/99-local.properties").exists()
    }
}

// https://help.sap.com/viewer/b2f400d4c0414461a4bb7e115dccd779/LATEST/en-US/784f9480cf064d3b81af9cad5739fecc.html
tasks.register<Copy>("enableModeltMock") {
    from("hybris/bin/custom/extras/modelt/extensioninfo.disabled")
    into("hybris/bin/custom/extras/modelt/")
    rename { "extensioninfo.xml" }
}

tasks.named("installManifestAddons") {
    mustRunAfter("generateLocalProperties")
}

tasks.register("setupLocalDevelopment") {
    group = "SAP Commerce"
    description = "Setup local development"
    dependsOn(
        "bootstrapPlatform",
        "bootstrapCloudhotfolder",
        "yinstallSolr",
        "generateLocalDeveloperProperties",
        "installManifestAddons",
        "enableModeltMock"
    )
}
