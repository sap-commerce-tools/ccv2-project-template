plugins {
    id("sap.commerce.build") version("3.0.0")
    id("sap.commerce.build.ccv2") version("3.0.0")
}
import mpern.sap.commerce.build.tasks.HybrisAntTask
import org.apache.tools.ant.taskdefs.condition.Os

repositories {
    flatDir { dirs("platform") }
    jcenter()
}

val generateLocalProperties by tasks.registering(WriteProperties::class) {
    comment = "GENEREATED AT " + java.time.Instant.now()
    outputFile = project.file("hybris/config/local.properties")

    property("hybris.optional.config.dir", project.file("hybris/config/local-config"))
}

tasks.named("installManifestAddons") {
    mustRunAfter(generateLocalProperties)
}

tasks.register("setupLocalDevelopment") {
    dependsOn("bootstrapPlatform", generateLocalProperties, "installManifestAddons")
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

tasks.register("generateCode") {
    dependsOn("generateDemoStorefront", "generateDemoOrderManagment", "generateDemoOcc", "generateDemoOccTests")
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
    from("hybris/config-template")
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
tasks.named("generateLocalProperties") {
    mustRunAfter("mergeConfigFolder")
}

tasks.register("setupConfigFolder") {
    dependsOn("symlinkCommonProperties", "symlinkLocalDevProperties", "generateLocalProperties")
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
tasks.register("generateProprietaryCode") {
    dependsOn("generateCode", "setupConfigFolder", "setupSolrConfigForLocalDevelopment")
}
