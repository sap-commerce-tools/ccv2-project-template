buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        "classpath"(group = "com.lihaoyi", name = "sjsonnet_2.13", version = "0.1.6")
    }
}
import sjsonnet.SjsonnetMain;
import java.lang.System;

tasks.register("jsonnet") {
    doLast {
        sjsonnet.SjsonnetMain.main0(
    arrayOf("foo.jsonnet"),
    sjsonnet.SjsonnetMain.createParseCache(),
    java.lang.System.`in`,
    java.lang.System.`out`,
    java.lang.System.`err`,
    os.package\$.MODULE\$.pwd(),
    scala.Option.apply(null)
);
    }
}