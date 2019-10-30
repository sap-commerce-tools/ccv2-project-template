plugins {
    id("mpern.sap.commerce.build") version("1.5.1")
    id("mpern.sap.commerce.build.ccv2") version("1.5.1")
}

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