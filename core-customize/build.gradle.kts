plugins {
    id("mpern.sap.commerce.build") version("2.0.0")
    id("mpern.sap.commerce.build.ccv2") version("2.0.0")
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
// For a regular project, just commit the config folder and your custom extensions (hybris/bin/custom) as usual!

// ant modulegen -Dinput.module=accelerator -Dinput.name=demoshop -Dinput.package=com.demo.shop
tasks.register<HybrisAntTask>("generateDemoStorefront") {
    dependsOn("bootstrapPlatform", "createDefaultConfig")

    args("modulegen")
    antProperty("input.module", "accelerator")
    antProperty("input.name", "demoshop")
    antProperty("input.package", "com.demo.shop")
}

// setup hybris/config folder
tasks.register<Copy>("mergeConfigFolder") {
    dependsOn("generateDemoStorefront")

    from("hybris/config-template")
    into("hybris/config")
}
tasks.register<Exec>("symlinkCommonProperties") {
    dependsOn("mergeConfigFolder")
     if (Os.isFamily(Os.FAMILY_UNIX)) {
        commandLine("sh", "-c", "ln -s ../environments/common.properties 10-local.properties")
    } else {
        // https://blogs.windows.com/windowsdeveloper/2016/12/02/symlinks-windows-10/
        commandLine("cmd", "/c", """mklink /d "10-local.properties" "..\\environments\\common.properties" """)
    }
    workingDir("hybris/config/local-config")
}
tasks.register<Exec>("symlinkLocalDevProperties") {
    dependsOn("mergeConfigFolder")
     if (Os.isFamily(Os.FAMILY_UNIX)) {
        commandLine("sh", "-c", "ln -s ../environments/local-dev.properties 50-local.properties")
    } else {
        // https://blogs.windows.com/windowsdeveloper/2016/12/02/symlinks-windows-10/
        commandLine("cmd", "/c", """mklink /d "50-local.properties" "..\\environments\\local-dev.properties" """)
    }
    workingDir("hybris/config/local-config")
}
tasks.named("generateLocalProperties") {
    mustRunAfter("mergeConfigFolder")
    dependsOn("symlinkCommonProperties", "symlinkLocalDevProperties")
}

// starting and stopping solr generates the default solr configuration
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
tasks.register<Exec>("symlinkSolrConfigForLocalDevelopment") {
    dependsOn("moveSolrConfig")
     if (Os.isFamily(Os.FAMILY_UNIX)) {
        commandLine("sh", "-c", "ln -s ../../../../../solr/server/solr/configsets configsets")
    } else {
        // https://blogs.windows.com/windowsdeveloper/2016/12/02/symlinks-windows-10/
        commandLine("cmd", "/c", """mklink /d "configsets" "..\\..\\..\\..\\..\\solr\\server\\solr\\configsets" """)
    }
    workingDir("hybris/config/solr/instances/cloud")
}

tasks.register("generateProprietaryCode") {
    dependsOn("symlinkSolrConfigForLocalDevelopment")
}
