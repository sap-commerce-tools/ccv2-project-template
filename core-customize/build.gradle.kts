plugins {
    id("sap.commerce.build") version("3.3.0")
    id("sap.commerce.build.ccv2") version("3.3.0")
}
import mpern.sap.commerce.build.tasks.HybrisAntTask
import org.apache.tools.ant.taskdefs.condition.Os

repositories {
    flatDir { dirs("platform") }
    mavenCentral()
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
