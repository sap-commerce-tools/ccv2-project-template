plugins {
    id("com.diffplug.spotless") version("5.17.0")
}

spotless {
    val projectName = "ccv2"
    val importOrderConfigFile = project.file("core-customize/conventions/ccv2-eclipse.importorder")
    val javaFormatterConfigFile = project.file("core-customize/conventions/ccv2-eclipse-formatter-settings.xml")
    val cssFormatterConfigFile = project.file("js-storefront/" + projectName + "/.prettierrc")
    val tsFormatterConfigFile = project.file("js-storefront/" + projectName + "/.prettierrc")

    java {
        target("core-customize/hybris/bin/custom/<projectname>/**/*.java")
        importOrderFile(importOrderConfigFile)
        eclipse().configFile(javaFormatterConfigFile)
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("spartacus-styles") {
        target("js-storefront/" + projectName + "/src/**/*.scss")
        prettier().config(mapOf("parser" to "css")).configFile(cssFormatterConfigFile)
    }

    format("spartacus-angular") {
        target("js-storefront/" + projectName + "/src/**/*.ts")
        prettier().config(mapOf("parser" to "typescript")).configFile(tsFormatterConfigFile)
    }
}
