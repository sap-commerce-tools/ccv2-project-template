plugins {
    id("sap.commerce.build") version("3.6.0")
    id("sap.commerce.build.ccv2") version("3.6.0")
    id("de.undercouch.download") version("4.1.2")
}
import mpern.sap.commerce.build.tasks.HybrisAntTask
import org.apache.tools.ant.taskdefs.condition.Os

import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify

import java.time.Instant
import java.util.Base64

val DEPENDENCY_FOLDER = "dependencies"
repositories {
    flatDir { dirs(DEPENDENCY_FOLDER) }
    mavenCentral()
}

//Optional: automate downloads from launchpad.support.sap.com
//  remove this block if you use something better, like Maven
//  Recommended reading: 
//  https://github.com/SAP/commerce-gradle-plugin/blob/master/docs/FAQ.md#downloadPlatform
if (project.hasProperty("sUser") && project.hasProperty("sUserPass")) {
    val SUSER = project.property("sUser") as String
    val SUSERPASS = project.property("sUserPass") as String
    val AUTHORIZATION = Base64.getEncoder().encodeToString((SUSER + ":" + SUSERPASS).toByteArray())

    val COMMERCE_VERSION = CCV2.manifest.commerceSuiteVersion
    val commerceSuiteDownloadUrl = project.property("com.sap.softwaredownloads.commerceSuite.${COMMERCE_VERSION}.downloadUrl")
    val commerceSuiteChecksum = project.property("com.sap.softwaredownloads.commerceSuite.${COMMERCE_VERSION}.checksum")
    
    tasks.register<Download>("downloadPlatform") {
        src(commerceSuiteDownloadUrl)
        dest(file("${DEPENDENCY_FOLDER}/hybris-commerce-suite-${COMMERCE_VERSION}.zip"))
        header("Authorization", "Basic ${AUTHORIZATION}")
        overwrite(false)
        tempAndMove(true)
        onlyIfModified(true)
        useETag(true)
    }

    tasks.register<Verify>("downloadAndVerifyPlatform") {
        dependsOn("downloadPlatform") 
        src(file("${DEPENDENCY_FOLDER}/hybris-commerce-suite-${COMMERCE_VERSION}.zip"))
        algorithm("SHA-256")
        checksum(commerceSuiteChecksum)
    }

    tasks.named("bootstrapPlatform") {
        dependsOn("downloadAndVerifyPlatform")
    }

    //check if Integration Extension Pack is configured and download it too
    if (CCV2.manifest.extensionPacks.any{"hybris-commerce-integrations".equals(it.name)}) {
        val INTEXTPACK_VERSION = CCV2.manifest.extensionPacks.first{"hybris-commerce-integrations".equals(it.name)}.version
        val commerceIntegrationsDownloadUrl = project.property("com.sap.softwaredownloads.commerceIntegrations.${INTEXTPACK_VERSION}.downloadUrl")
        val commerceIntegrationsChecksum = project.property("com.sap.softwaredownloads.commerceIntegrations.${INTEXTPACK_VERSION}.checksum")
        
        tasks.register<Download>("downloadIntExtPack") {
            src(commerceIntegrationsDownloadUrl)
            dest(file("${DEPENDENCY_FOLDER}/hybris-commerce-integrations-${INTEXTPACK_VERSION}.zip"))
            header("Authorization", "Basic ${AUTHORIZATION}")
            overwrite(false)
            tempAndMove(true)
            onlyIfModified(true)
            useETag(true)
        }

        tasks.register<Verify>("downloadAndVerifyIntExtPack") {
            dependsOn("downloadIntExtPack")
            src(file("${DEPENDENCY_FOLDER}/hybris-commerce-integrations-${INTEXTPACK_VERSION}.zip"))
            algorithm("SHA-256")
            checksum(commerceIntegrationsChecksum)
        }

        tasks.named("bootstrapPlatform") {
            dependsOn("downloadAndVerifyIntExtPack")
        }
    }
}

tasks.register<WriteProperties>("generateLocalProperties") {
    comment = "GENEREATED AT " + Instant.now()
    outputFile = project.file("hybris/config/local.properties")
    property("hybris.optional.config.dir", project.file("hybris/config/local-config").absolutePath)
    doLast {
        mkdir(project.file("hybris/config/local-config/"))
    }
}

val symlinkConfigTask = tasks.register("symlinkConfig")
val localConfig = file("hybris/config/local-config")
mapOf(
    "10-local.properties" to file("hybris/config/cloud/common.properties"),
    "20-local.properties" to file("hybris/config/cloud/persona/development.properties"),
    "50-local.properties" to file("hybris/config/cloud/local-dev.properties")
).forEach{
    val symlinkTask = tasks.register<Exec>("symlink${it.key}") {
        val path = it.value.relativeTo(localConfig)
        if (Os.isFamily(Os.FAMILY_UNIX)) {
            commandLine("sh", "-c", "ln -sfn ${path} ${it.key}")
        } else {
            // https://blogs.windows.com/windowsdeveloper/2016/12/02/symlinks-windows-10/
            val windowsPath = path.toString().replace("[/]".toRegex(), "\\")
            commandLine("cmd", "/c", """mklink /d "${it.key}" "${windowsPath}" """)
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
    outputFile = project.file("hybris/config/local-config/99-local.properties")
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
    dependsOn("bootstrapPlatform", "generateLocalDeveloperProperties", "installManifestAddons", "enableModeltMock")
}
