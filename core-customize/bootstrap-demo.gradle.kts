plugins {
    id("de.undercouch.download") version("4.1.1")
    id("sap.commerce.build") version("3.5.0")
    id("sap.commerce.build.ccv2") version("3.5.0")
}
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.databricks:sjsonnet_2.13:0.4.0")
        classpath("io.github.java-diff-utils:java-diff-utils:4.5")
    }
}
import mpern.sap.commerce.build.tasks.HybrisAntTask
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.DiffUtils

val bootstrapDemo = tasks.register("bootstrapDemo") {
    group = "Boostrap"
    description = "Bootstrap demo project based on 'cx' recipe"
}

defaultTasks("bootstrapDemo")

tasks.register<GradleBuild>("bootstrapDefaultProject") {
    buildFile = file("bootstrap.gradle.kts")
    startParameter.projectProperties = mapOf(
        "projectName" to "demoshop",
        "rootPackage" to "com.demo.shop"
    )
}
bootstrapDemo.configure {
    dependsOn("bootstrapDefaultProject")
}
tasks.register("fixcmsflexcomponent") {
    dependsOn("bootstrapDefaultProject")
    doLast {
        ant.withGroovyBuilder {
            "touch"("file" to "hybris/bin/custom/demoshop/demoshopstorefront/web/webroot/WEB-INF/views/responsive/cms/cmsflexcomponent.jsp")
        }
    }
}
tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadSpartacusSampleData") {
    src("https://github.com/SAP/spartacus/releases/download/storefront-3.2.0/spartacussampledata.2005.zip")
    dest("dependencies")
    onlyIfModified(true)
    useETag(true)
}

tasks.register<Copy>("unpackSpartacus") {
    dependsOn("downloadSpartacusSampleData", "bootstrapDefaultProject")
    from(zipTree("dependencies/spartacussampledata.2005.zip"))
    into("hybris/bin/custom")
    eachFile {
        val newPath = relativePath.segments.drop(1).toMutableList()
        newPath.add(0, "spartacussampledata")
        relativePath = RelativePath(true, *newPath.toTypedArray())  
    }
    includeEmptyDirs = false
}
// ant extgen -Dinput.template=yacceleratorordermanagement -Dinput.name=demoshopordermanagement -Dinput.package=com.demo.shop.ordermanagement
tasks.register<HybrisAntTask>("generateDemoOrderManagment") {
    dependsOn("bootstrapDefaultProject")

    args("extgen")
    antProperty("input.template", "yacceleratorordermanagement")
    antProperty("input.name", "demoshopordermanagement")
    antProperty("input.package", "com.demo.shop.ordermanagement")
}
tasks.register("extraExtensions") {
    dependsOn("unpackSpartacus", "generateDemoOrderManagment", "fixcmsflexcomponent")
}

val extrasFolder = file("bootstrap/demo/config-extras")
val configFolder = file("hybris/config")

tasks.register<Copy>("copyExtraConfig") {
    dependsOn("extraExtensions")
    from(extrasFolder) {
        exclude("**/*.properties")
    }
    into(configFolder)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val patchProps = tasks.register("patchProperties")

extrasFolder
.walk()
.filter{it.isFile() && it.extension.equals("properties")}
.forEach{
    val relative = it.relativeTo(extrasFolder)
    val target = configFolder.resolve(relative)
    val patch = tasks.register("patchProperties_${relative.toString().replace("[/\\\\]".toRegex(), "-")}") {
        dependsOn("extraExtensions")
        doLast {
            var newContent = ""
            if (target.exists()) {
                newContent = target.readText()
                newContent += "\n"
            }
            newContent += it.readText()
            target.writeText(newContent)
        }
    }
    patchProps.configure{
        dependsOn(patch)
    }
}

val manifestJsonnet = file("manifest.jsonnet")
tasks.register("patchManifestJsonnet") {
    mustRunAfter("bootstrapDefaultProject")
    doLast {
        val original = manifestJsonnet.readLines()
        val diff = file("bootstrap/demo/manifest.jsonnet.patch").readLines()

        val patch = UnifiedDiffUtils.parseUnifiedDiff(diff);
        val result = DiffUtils.patch(original, patch);

        manifestJsonnet.writeText(result.joinToString("\n"))
    }
}
tasks.register("regenerateManifest") {
    dependsOn("patchManifestJsonnet")
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
tasks.register("updateConfiguration") {
    dependsOn("copyExtraConfig", "patchProperties", "regenerateManifest")
}

bootstrapDemo.configure {
    dependsOn("updateConfiguration")
    doLast {
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println("!!! To finish the setup please run  !!!")
        println("!!! ./gradlew setupLocalDevelopment !!!")
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    }
}
