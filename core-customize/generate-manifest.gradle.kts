buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        "classpath"(group = "com.lihaoyi", name = "sjsonnet_2.13", version = "0.1.6")
    }
}

tasks.register("generateManifest") {
    group = "Bootstrap"
    description = "Generate manifest.json using manifest-generator.jsonnet"
    doLast {
        sjsonnet.SjsonnetMain.main0(
            arrayOf("--output-file", "manifest.json", "manifest-generator.jsonnet"),
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

defaultTasks("generateManifest")
