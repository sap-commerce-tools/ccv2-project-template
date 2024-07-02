plugins {
    id("sap.commerce.build") version("4.0.0")
    id("sap.commerce.build.ccv2") version("4.0.0")
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.databricks:sjsonnet_2.13:0.4.0")
    }
}
import mpern.sap.commerce.build.tasks.HybrisAntTask
import org.apache.tools.ant.taskdefs.condition.Os

repositories {
    flatDir { dirs("dependencies") }
    mavenCentral()
}

val azureCloudExtensionsDisabled = project.hasProperty("azureCloudExtensionsDisabled") && (project.property("azureCloudExtensionsDisabled") == "true")
val accStorefrontEnabled = project.hasProperty("accStorefrontEnabled") && (project.property("accStorefrontEnabled") == "true")

fun inputName(): String {
    if (!(project.hasProperty("projectName") && project.hasProperty("rootPackage"))) {
        logger.error("Please provide the projectName and rootPacakge")
        logger.error("e.g. ./gradlew -b bootstrap.gradle.kts -PprojectName=coolshop -ProotPackage=com.shop.cool")
        throw InvalidUserDataException("Please provide projectName / rootPackage!")
    }
    return (project.property("projectName") as String)
}

fun inputPackage(): String {
    if (!(project.hasProperty("projectName") && project.hasProperty("rootPackage"))) {
        logger.error("Please provide the projectName and rootPacakge")
        logger.error("e.g. ./gradlew -b bootstrap.gradle.kts -PprojectName=coolshop -ProotPackage=com.shop.cool")
        throw InvalidUserDataException("Please provide projectName / rootPackage!")
    }
    return (project.property("rootPackage") as String)
}

apply(from = "bootstrap-extras.gradle.kts")

tasks.named("createDefaultConfig") {
    dependsOn("bootstrapExtras")
}

//** generate code
// ant modulegen -Dinput.module=accelerator -Dinput.name=demoshop -Dinput.package=com.demo.shop
tasks.register<HybrisAntTask>("generateAcceleratorModule") {
    dependsOn("bootstrapPlatform", "createDefaultConfig")

    args("modulegen")
    antProperty("input.module", "accelerator")
    antProperty("input.name", inputName())
    antProperty("input.package", inputPackage())
}

if (accStorefrontEnabled) {
    tasks.register<Copy>("copyConfigImpex") {
        dependsOn("generateAcceleratorModule", "copyJsonnet")
        from("bootstrap/")
        include("*.impex")
        into("hybris/bin/custom/${inputName()}/${inputName()}storefront/resources/impex/")
    }
} else {
    tasks.register<Delete>("deleteAcceleratorStorefrontExtension") {
        dependsOn("generateAcceleratorModule")
        delete("hybris/bin/custom/${inputName()}/${inputName()}storefront/")
    }
}

tasks.register<HybrisAntTask>("generateOcc") {
    dependsOn("bootstrapPlatform", "createDefaultConfig")

    args("extgen")
    antProperty("input.template", "yocc")
    antProperty("input.name", "${inputName()}occ")
    antProperty("input.package", "${inputPackage()}.occ")
}

tasks.register<HybrisAntTask>("generateOccTests") {
    dependsOn("bootstrapPlatform", "createDefaultConfig")

    args("extgen")
    antProperty("input.template", "yocctests")
    antProperty("input.name", "${inputName()}occtests")
    antProperty("input.package", "${inputPackage()}.occ.tests")
}

tasks.register("generateCode") {
    dependsOn("generateOcc", "generateOccTests")
    doLast {
        ant.withGroovyBuilder {
            "move"("file" to "hybris/bin/custom/${inputName()}occ", "todir" to "hybris/bin/custom/${inputName()}")
            "move"("file" to "hybris/bin/custom/${inputName()}occtests", "todir" to "hybris/bin/custom/${inputName()}")
        }
    }
}

if (accStorefrontEnabled) {
    tasks.named("generateCode") {
        dependsOn("copyConfigImpex")
    }
} else {
    tasks.named("generateCode") {
        dependsOn("deleteAcceleratorStorefrontExtension")
    }
}

//** setup hybris/config folder
tasks.register<Copy>("mergeConfigFolder") {
    dependsOn("generateCode")
    mustRunAfter("copyJsonnet")
    from("bootstrap/config-template")
    into("hybris/config")
    filter(org.apache.tools.ant.filters.ReplaceTokens::class, "tokens" to mapOf("projectName" to inputName()))
    if (!accStorefrontEnabled) {
        filter { line -> line.replace(Regex("^.*${inputName()}storefront.*$"), "") }
    }
    if (azureCloudExtensionsDisabled) {
        filter { line -> line.replace(Regex("^.*azurecloudhotfolder.*$"), "") }
    }
}

tasks.register<Copy>("copyJsonnet") {
    from("bootstrap/manifest.jsonnet")
    into(".")
}

fun generateManifest() {
    val intExtJsonnetParams = if (project.hasProperty("intExtPackVersion")) {
        arrayOf("--ext-str", "intExtPackVersion='${project.property("intExtPackVersion")}'")
    } else arrayOf()

    val accStorefrontParams = if (accStorefrontEnabled) {
        arrayOf("--ext-str", "storefrontExtension=${inputName()}storefront", "--ext-code", "accStorefrontEnabled=true")
    } else arrayOf()

    val solrVersionParams = if (project.hasProperty("solrVersion")) {
        arrayOf("--ext-str", "solrVersion='${project.property("solrVersion")}'")
    } else arrayOf()

    sjsonnet.SjsonnetMain.main0(
            accStorefrontParams + intExtJsonnetParams + solrVersionParams + arrayOf("--output-file", "manifest.json", "manifest.jsonnet"),
            sjsonnet.SjsonnetMain.createParseCache(),
            java.lang.System.`in`,
            java.lang.System.`out`,
            java.lang.System.err,
            os.Path(project.rootDir.toPath()),
            scala.`None$`.empty(),
            scala.`None$`.empty()
    )
}

tasks.register("generateManifest") {
    dependsOn("copyJsonnet")
    doLast {
        generateManifest()
    }
}

val localDev = mapOf(
    "10-local.properties" to file("hybris/config/cloud/common.properties"),
    "20-local.properties" to file("hybris/config/cloud/persona/development.properties"),
    "50-local.properties" to file("hybris/config/cloud/local-dev.properties")
)
val localConfig = file("hybris/config/local-config")
val symlink = tasks.register("symlinkConfig")
localDev.forEach {
    val singleLink = tasks.register<Exec>("symlink${it.key}") {
        dependsOn("mergeConfigFolder")
        val path = it.value.relativeTo(localConfig)
        if (Os.isFamily(Os.FAMILY_UNIX)) {
            commandLine("sh", "-c", "ln -sfn $path ${it.key}")
        } else {
            // https://blogs.windows.com/windowsdeveloper/2016/12/02/symlinks-windows-10/
            val windowsPath = path.toString().replace("[/]".toRegex(), "\\")
            commandLine("cmd", "/c", """mklink /d "${it.key}" "$windowsPath" """)
        }
        workingDir(localConfig)
    }
    symlink.configure {
        dependsOn(singleLink)
    }
}

tasks.register<WriteProperties>("generateLocalProperties") {
    mustRunAfter("mergeConfigFolder")
    comment = "GENEREATED AT " + java.time.Instant.now()
    destinationFile = project.file("hybris/config/local.properties")

    property("hybris.optional.config.dir", project.file("hybris/config/local-config"))
}

tasks.register("setupConfigFolder") {
    dependsOn(symlink, "generateLocalProperties", "generateManifest")
}

tasks.register<GradleBuild>("setupLocalDev") {
    mustRunAfter("generateCode", "setupConfigFolder")
    dir = rootDir
    tasks = listOf("setupLocalDevelopment")
}

//** combine all of the above
tasks.register("bootstrapNewProject") {
    dependsOn("generateCode", "setupConfigFolder", "setupLocalDev")
    group = "Bootstrap"
    description = "Bootstrap a new SAP Commerce project"
    doLast {
        println("")
        println("==== Project generation finished! ====")
        println("- Generated extensions:")
        file("hybris/bin/custom/${inputName()}").listFiles().sortedBy { it.name }.forEach {
            println("\t${it.name}")
        }
        if (project.hasProperty("intExtPackVersion")) {
            println("- Configured Integration Extension Pack ${project.property("intExtPackVersion")}")
        }
        println("- Generated new manifest.json (using manifest.jsonnet)")
        println("")
        if (!accStorefrontEnabled) {
            println("! Deprecated accelerator storefront has not been included in the generated code.")
            println("! If you want to use the JSP based deprecated accelerator storefront,")
            println("! provide the flag -DaccStorefrontEnabled=true to the bootstrap command.")
            println("")
        }
        println("? (optional)")
        println("? If you plan to customize the Solr configuration of your project, please execute:")
        println("? ./gradlew -b bootstrap.gradle.kts enableSolrCustomization")
    }
}

defaultTasks("bootstrapNewProject")

//******* Optional: Bootstrap Solr customization *******
tasks.register("enableSolrCustomization") {
    dependsOn("symlinkSolrConfig", "manifestWithSolr")
    group = "Bootstrap"
    description = "Prepare Solr configuration for both local development and customization"
}

tasks.register<HybrisAntTask>("startSolr") {
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
        file("solr/server/solr").mkdirs()
        ant.withGroovyBuilder {
            "move"("file" to "hybris/config/solr/instances/cloud/configsets", "todir" to "solr/server/solr")
        }
    }
}

tasks.register<Exec>("symlinkSolrConfig") {
    dependsOn("moveSolrConfig")
    if (Os.isFamily(Os.FAMILY_UNIX)) {
        commandLine("sh", "-c", "ln -sfn ../../../../../solr/server/solr/configsets configsets")
    } else {
        // https://blogs.windows.com/windowsdeveloper/2016/12/02/symlinks-windows-10/
        commandLine("cmd", "/c", """mklink /d "configsets" "..\\..\\..\\..\\..\\solr\\server\\solr\\configsets" """)
    }
    workingDir("hybris/config/solr/instances/cloud")
}

// Solr Version Selection
// https://help.sap.com/viewer/1be46286b36a4aa48205be5a96240672/latest/en-US/b35bc14a62aa4950bdba451a5f40fc61.html#loiod7294323e5e542b7b37f48dd83565321
tasks.register("findSolrVersion") {
    dependsOn("bootstrapPlatform")
    doLast {
        val solrDir = file("hybris/bin/modules/search-and-navigation/solrserver/resources/solr")
                .listFiles()
                .filter { it.isDirectory() }
                .sortedBy { it.name }.last()
        val props = java.util.Properties();
        props.load(solrDir.resolve("server/meta.properties").inputStream())
        extra.set("bundledSolrVersion", props.get("version"))
    }
}

tasks.register("manifestWithSolr") {
    mustRunAfter("symlinkSolrConfig")
    dependsOn("findSolrVersion")
    doLast {
        println("Regenerating manifest.json...")
        val solrVersion = tasks.named("findSolrVersion").get().extra.get("bundledSolrVersion") as String
        val majorMinor = solrVersion.split(".").take(2).joinToString(".")
        println("Detected Solr version $solrVersion bundled with the platform.")
        println("Pinning Solr version to $majorMinor in manifest.json")
        ant.withGroovyBuilder {
            "replace"(
                    "file" to "manifest.jsonnet",
                    "token" to "solrVersion=null",
                    "value" to "solrVersion='${majorMinor}'"
            )
        }
        generateManifest()
    }
}
