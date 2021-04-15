plugins {
    id("sap.commerce.build") version("3.4.0")
    id("sap.commerce.build.ccv2") version("3.4.0")
    id("de.undercouch.download") version("4.1.1")
}
import mpern.sap.commerce.build.tasks.HybrisAntTask
import org.apache.tools.ant.taskdefs.condition.Os

import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify

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
    tasks.register<Download>("downloadPlatform") {
        src("https://softwaredownloads.sap.com/file/0020000000342432021")
        dest(file("${DEPENDENCY_FOLDER}/hybris-commerce-suite-${CCV2.manifest.commerceSuiteVersion}.zip"))
        username(SUSER)
        password(SUSERPASS)
        overwrite(false)
        tempAndMove(true)
        onlyIfModified(true)
        useETag(true)
    }

    tasks.register<Verify>("downloadAndVerifyPlatform") {
        dependsOn("downloadPlatform")
        src(file("dependencies/hybris-commerce-suite-${CCV2.manifest.commerceSuiteVersion}.zip"))
        algorithm("SHA-256")
        checksum("5a96db9d91b5136d48f742ac0575981bbf11aadd79e2a45e357cdf9a8b3d434b")
    }

    tasks.named("bootstrapPlatform") {
        dependsOn("downloadAndVerifyPlatform")
    }

    //check if Integration Extension Pack is configured and download it too
    if (CCV2.manifest.extensionPacks.any{"hybris-commerce-integrations".equals(it.name)}) {
        tasks.register<Download>("downloadIntExtPack") {
            src("https://softwaredownloads.sap.com/file/0020000000267192021")
            dest(file("${DEPENDENCY_FOLDER}/hybris-commerce-integrations-${CCV2.manifest.extensionPacks.first{"hybris-commerce-integrations".equals(it.name)}.version}.zip"))
            username(SUSER)
            password(SUSERPASS)
            overwrite(false)
            tempAndMove(true)
            onlyIfModified(true)
            useETag(true)
        }

        tasks.register<Verify>("downloadAndVerifyIntExtPack") {
            dependsOn("downloadIntExtPack")
            src(file("dependencies/hybris-commerce-integrations-${CCV2.manifest.extensionPacks.first{"hybris-commerce-integrations".equals(it.name)}.version}.zip"))
            algorithm("SHA-256")
            checksum("abbc1abaea29241a9af2fc50516b36c7d782091121927378028012343d1e0bf1")
        }

        tasks.named("bootstrapPlatform") {
            dependsOn("downloadAndVerifyIntExtPack")
        }
    }
}

tasks.register<WriteProperties>("generateLocalProperties") {
    comment = "GENEREATED AT " + java.time.Instant.now()
    outputFile = project.file("hybris/config/local.properties")

    property("hybris.optional.config.dir", "\${HYBRIS_CONFIG_DIR}/local-config")
}

// https://help.sap.com/viewer/b2f400d4c0414461a4bb7e115dccd779/LATEST/en-US/784f9480cf064d3b81af9cad5739fecc.html
tasks.register("enableModeltMock") {
    onlyIf {
        !file("hybris/bin/custom/extras/modelt/extensioninfo.xml").exists()
    }
    doLast {
         ant.withGroovyBuilder {
            "copy"("file" to "hybris/bin/custom/extras/modelt/extensioninfo.disabled", "tofile" to "hybris/bin/custom/extras/modelt/extensioninfo.xml")
        }
    }
}

tasks.named("installManifestAddons") {
    mustRunAfter("generateLocalProperties")
}

tasks.register("setupLocalDevelopment") {
    group = "SAP Commerce"
    description = "Setup local development"
    dependsOn("bootstrapPlatform", "generateLocalProperties", "installManifestAddons", "enableModeltMock")
}
