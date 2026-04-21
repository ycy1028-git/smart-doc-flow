plugins {
    base
}

allprojects {
    group = "io.github.smartdocflow"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    if (name != "smartdoc-flow-service") {
        apply(plugin = "java-library")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(21)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
