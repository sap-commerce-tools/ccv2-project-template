plugins {
    id("sap.commerce.build") version("3.0.0")
    id("sap.commerce.build.ccv2") version("3.0.0")
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

if (!(project.hasProperty("projectName") && project.hasProperty("rootPackage"))) {
    logger.error("Please provide the projectName and rootPacakge")
    logger.error("e.g. ./gradlew -b bootstrap.gradle.kts -PprojectName=coolshop -ProotPackage=com.shop.cool")
    throw InvalidUserDataException("Please provide projectName / rootPackage!")
}

val inputName = project.property("projectName") as String
val inputPackage = project.property("rootPackage") as String

//** generate code
// ant modulegen -Dinput.module=accelerator -Dinput.name=demoshop -Dinput.package=com.demo.shop
tasks.register<HybrisAntTask>("generateNewStorefront") {
    dependsOn("bootstrapPlatform", "createDefaultConfig")

    args("modulegen")
    antProperty("input.module", "accelerator")
    antProperty("input.name", inputName)
    antProperty("input.package", inputPackage)
}

// ant extgen -Dinput.template=yocc -Dinput.name=demoshopocc -Dinput.package=com.demo.shop.occ
tasks.register<HybrisAntTask>("generateOcc") {
    dependsOn("bootstrapPlatform", "createDefaultConfig")

    args("extgen")
    antProperty("input.template", "yocc")
    antProperty("input.name", "${inputName}occ")
    antProperty("input.package", "${inputPackage}.occ.tests")
}

// ant extgen -Dinput.template=yocc -Dinput.name=demoshopocc -Dinput.package=com.demo.shop.occ.tests
tasks.register<HybrisAntTask>("generateOccTests") {
    dependsOn("bootstrapPlatform", "createDefaultConfig")

    args("extgen")
    antProperty("input.template", "yocctests")
    antProperty("input.name", "${inputName}occtests")
    antProperty("input.package", "${inputPackage}.occ.tests")
}

tasks.register("generateCode") {
    dependsOn("generateNewStorefront", "generateOcc", "generateOccTests")
    doLast {
         ant.withGroovyBuilder {
            "move"("file" to "hybris/bin/custom/${inputName}occ", "todir" to "hybris/bin/custom/${inputName}")
            "move"("file" to "hybris/bin/custom/${inputName}occtests", "todir" to "hybris/bin/custom/${inputName}")
        }
    }
}

//** setup hybris/config folder
tasks.register<Copy>("mergeConfigFolder") {
    mustRunAfter("generateCode")
    from("bootstrap/config-template")
    into("hybris/config")
    filter(org.apache.tools.ant.filters.ReplaceTokens::class, "tokens" to mapOf("projectName" to inputName))
}
tasks.register<Copy>("filterJsonnet") {
    from("bootstrap/manifest.jsonnet")
    into("bootstrap")
    rename(".*", "manifest.jsonnet.filtered")
    filter(org.apache.tools.ant.filters.ReplaceTokens::class, "tokens" to mapOf("projectName" to inputName))
}
tasks.register("moveJsonnet") {
    dependsOn("filterJsonnet")
    doLast {
        ant.withGroovyBuilder {
            "move"("file" to "bootstrap/manifest.jsonnet.filtered", "tofile" to file("manifest.jsonnet"))
        }
    }
}
tasks.register("generateManifest") {
    dependsOn("moveJsonnet")
    doLast {
        sjsonnet.SjsonnetMain.main0(
            arrayOf("--output-file", "manifest.json", "manifest.jsonnet"),
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
tasks.register("bootstrapNewProject") {
    dependsOn("generateCode", "setupConfigFolder", "setupSolrConfigForLocalDevelopment")
    doLast {
        println("==== Project generation finished! ====")
        println("- Generated extensions:")
        file("hybris/bin/custom/${inputName}").listFiles().forEach {
            println("\t${it.name}")
        }
        println("- Generated new manifest.json (using manifest.jsonnet)")
        println("")
        println("To finish the setup, please execute:")
        println("./gradlew setupLocalDevelopment")
    }
}

defaultTasks("bootstrapNewProject")