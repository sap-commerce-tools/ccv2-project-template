import java.net.http.*
import java.time.Duration
import java.net.URI
import groovy.json.JsonSlurper
import de.undercouch.gradle.tasks.download.Download

buildscript {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("de.undercouch:gradle-download-task:4.1.1")
    }
}

val EXTRAS_SOURCE = "build/extras"
val EXTRAS_TARGET = "hybris/bin/custom/extras"
val extraExtensions = mapOf(
    "sanecleanup" to "https://api.github.com/repos/sap-commerce-tools/sanecleanup/releases/latest",
    "environment-ribbon" to "https://api.github.com/repos/sap-commerce-tools/environment-ribbon/releases/latest",
    "hacvcsinfo" to "https://api.github.com/repos/sap-commerce-tools/hacvcsinfo/releases/latest"
)
val allExtras = tasks.register("bootstrapExtras") {
    description = "Download and unpack extras extension"
    group = "Bootstrap"
}

val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(30))
        .build()
extraExtensions.forEach {
    val url = tasks.register("extras_getLatest_${it.key}") {
        doLast {
            val request = HttpRequest.newBuilder(URI(it.value))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val body = response.body()
            val parsed = JsonSlurper().parseText(body) as Map<String, Object>
            extra.set("zipball", parsed["zipball_url"])
        }
    }
    val down = tasks.register<Download>("extras_download_${it.key}") {
        dependsOn(url)

        src(provider({url.get().extra.get("zipball")}))
        dest(file("${EXTRAS_SOURCE}/${it.key}.zip"))
        overwrite(true)
        tempAndMove(true)
        onlyIfModified(true)
        useETag(true)
    }

    val bootstrap = tasks.register("extras_bootstrap_${it.key}") {
        dependsOn(down)
        doLast {
            ant.withGroovyBuilder {
                "delete"("includeEmptyDirs" to true) {
                    "fileset"("dir" to "${EXTRAS_TARGET}/${it.key}", "defaultexcludes" to false, "erroronmissingdir" to false)
                }
            }
            copy {
                from(zipTree("${EXTRAS_SOURCE}/${it.key}.zip"))
                into(EXTRAS_TARGET)
                eachFile {
                    val newPath = relativePath.segments.drop(1).toMutableList()
                    newPath.add(0, it.key)
                    relativePath = RelativePath(true, *newPath.toTypedArray())  
                }
                includeEmptyDirs = false
            }
        }
    }
    allExtras.configure{
        dependsOn(bootstrap)
    }
}