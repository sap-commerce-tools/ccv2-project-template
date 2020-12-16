plugins {
    id("sap.commerce.build") version("3.0.0")
    id("sap.commerce.build.ccv2") version("3.0.0")
    id("de.undercouch.download") version("4.1.1")
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        "classpath"(group = "com.lihaoyi", name = "sjsonnet_2.13", version = "0.1.6")
    }
}

import mpern.sap.commerce.build.tasks.HybrisAntTask
import org.apache.tools.ant.taskdefs.condition.Os

repositories {
    flatDir { dirs("platform") }
    jcenter()
}

// ---------------------------------------------------
// Helper tasks to boostrap the project from scratch.
// *Those are only necessary because I don't want to add any properietary files owned by SAP to Github.*

//** generate code
// ant modulegen -Dinput.module=accelerator -Dinput.name=demoshop -Dinput.package=com.demo.shop
tasks.register<HybrisAntTask>("generateDemoStorefront") {
    dependsOn("bootstrapPlatform", "createDefaultConfig")

    args("modulegen")
    antProperty("input.module", "accelerator")
    antProperty("input.name", "demoshop")
    antProperty("input.package", "com.demo.shop")
}

// ant extgen -Dinput.template=yacceleratorordermanagement -Dinput.name=demoshopordermanagement -Dinput.package=com.demo.shop.ordermanagement
tasks.register<HybrisAntTask>("generateDemoOrderManagment") {
    dependsOn("bootstrapPlatform", "createDefaultConfig")

    args("extgen")
    antProperty("input.template", "yacceleratorordermanagement")
    antProperty("input.name", "demoshopordermanagement")
    antProperty("input.package", "com.demo.shop.ordermanagement")
}

// ant extgen -Dinput.template=yocc -Dinput.name=demoshopocc -Dinput.package=com.demo.shop.occ
tasks.register<HybrisAntTask>("generateDemoOcc") {
    dependsOn("bootstrapPlatform", "createDefaultConfig")

    args("extgen")
    antProperty("input.template", "yocc")
    antProperty("input.name", "demoshopocc")
    antProperty("input.package", "com.demo.shop.occ")
}

// ant extgen -Dinput.template=yocc -Dinput.name=demoshopocc -Dinput.package=com.demo.shop.occ.tests
tasks.register<HybrisAntTask>("generateDemoOccTests") {
    dependsOn("bootstrapPlatform", "createDefaultConfig")

    args("extgen")
    antProperty("input.template", "yocctests")
    antProperty("input.name", "demoshopocctests")
    antProperty("input.package", "com.demo.shop.occ.tests")
}

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadSpartacusAddon") {
    src("https://github.com/SAP/spartacus/releases/download/storefront-2.1.4/spartacussampledata.2005.zip")
    dest("platform")
}

tasks.register<Copy>("unpackSpartacus") {
    dependsOn("downloadSpartacusAddon")
    from(zipTree("platform/spartacussampledata.2005.zip"))
    into("hybris/bin/custom")
    eachFile {
        val newPath = relativePath.segments.drop(1).toMutableList()
        newPath.add(0, "spartacussampledataaddon")
        relativePath = RelativePath(true, *newPath.toTypedArray())  
    }
    includeEmptyDirs = false
}

tasks.register("generateCode") {
    dependsOn("generateDemoStorefront", "generateDemoOrderManagment", "generateDemoOcc", "generateDemoOccTests", "unpackSpartacus")
        doLast {
        ant.withGroovyBuilder {
            "move"("file" to "hybris/bin/custom/demoshopordermanagement", "todir" to "hybris/bin/custom/demoshop")
        }
        ant.withGroovyBuilder {
            "move"("file" to "hybris/bin/custom/demoshopocc", "todir" to "hybris/bin/custom/demoshop")
        }
        ant.withGroovyBuilder {
            "move"("file" to "hybris/bin/custom/demoshopocctests", "todir" to "hybris/bin/custom/demoshop")
        }
    }
}

//** setup hybris/config folder
tasks.register<Copy>("mergeConfigFolder") {
    mustRunAfter("generateCode")
    from("bootstrap/demo/config-template")
    into("hybris/config")
}
tasks.register<Exec>("symlinkCommonProperties") {
    dependsOn("mergeConfigFolder")
    if (Os.isFamily(Os.FAMILY_UNIX)) {
        commandLine("sh", "-c", "ln -sfn ../environments/common.properties 10-local.properties")
    } else {
        // https://blogs.windows.com/windowsdeveloper/2016/12/02/symlinks-windows-10/
        commandLine("cmd", "/c", """mklink /d "10-local.properties" "..\\environments\\common.properties" """)
    }
    workingDir("hybris/config/local-config")
}
tasks.register<Exec>("symlinkLocalDevProperties") {
    dependsOn("mergeConfigFolder")
     if (Os.isFamily(Os.FAMILY_UNIX)) {
        commandLine("sh", "-c", "ln -sfn ../environments/local-dev.properties 50-local.properties")
    } else {
        // https://blogs.windows.com/windowsdeveloper/2016/12/02/symlinks-windows-10/
        commandLine("cmd", "/c", """mklink /d "50-local.properties" "..\\environments\\local-dev.properties" """)
    }
    workingDir("hybris/config/local-config")
}
tasks.register<WriteProperties>("generateLocalProperties") {
    mustRunAfter("mergeConfigFolder")
    comment = "GENEREATED AT " + java.time.Instant.now()
    outputFile = project.file("hybris/config/local.properties")

    property("hybris.optional.config.dir", project.file("hybris/config/local-config"))
}

tasks.register("generateManifest") {
    doLast {
        sjsonnet.SjsonnetMain.main0(
            arrayOf("--output-file", "manifest.json", "bootstrap/demo/manifest.jsonnet"),
            sjsonnet.SjsonnetMain.createParseCache(),
            java.lang.System.`in`,
            java.lang.System.`out`,
            java.lang.System.err,
            os.Path(project.rootDir.toPath()),
            scala.`None$`.empty(),
            scala.`None$`.empty()
        )
    }
}

tasks.register("setupConfigFolder") {
    dependsOn("symlinkCommonProperties", "symlinkLocalDevProperties", "generateLocalProperties", "generateManifest")
}

//** bootstrap Solr configuration
tasks.register<HybrisAntTask>("startSolr") {
    dependsOn("mergeConfigFolder", "generateLocalProperties")
    args("startSolrServers")
}
tasks.register<HybrisAntTask>("stopSolr") {
    args("stopSolrServers")
    mustRunAfter("startSolr")
}
tasks.register("startStopSolr") {
    dependsOn("startSolr", "stopSolr")
}
tasks.register("moveSolrConfig") {
    dependsOn("startStopSolr")
    doLast {
        ant.withGroovyBuilder {
            "move"("file" to "hybris/config/solr/instances/cloud/configsets", "todir" to "solr/server/solr")
        }
    }
}
tasks.register<Exec>("setupSolrConfigForLocalDevelopment") {
    dependsOn("moveSolrConfig")
     if (Os.isFamily(Os.FAMILY_UNIX)) {
        commandLine("sh", "-c", "ln -sfn ../../../../../solr/server/solr/configsets configsets")
    } else {
        // https://blogs.windows.com/windowsdeveloper/2016/12/02/symlinks-windows-10/
        commandLine("cmd", "/c", """mklink /d "configsets" "..\\..\\..\\..\\..\\solr\\server\\solr\\configsets" """)
    }
    workingDir("hybris/config/solr/instances/cloud")
}

//** combine all of the above
tasks.register("bootstrapDemo") {
    dependsOn("generateCode", "setupConfigFolder", "setupSolrConfigForLocalDevelopment")
}

defaultTasks("bootstrapDemo")