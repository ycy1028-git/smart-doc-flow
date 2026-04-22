plugins {
    base
}

import org.gradle.api.publish.maven.MavenPublication

allprojects {
    group = "io.ycy.smartdocflow"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    if (name != "smartdoc-flow-service") {
        apply(plugin = "java-library")
        apply(plugin = "maven-publish")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(21)
        }

        extensions.configure<org.gradle.api.publish.PublishingExtension> {
            publications {
                create("mavenJava", MavenPublication::class.java) {
                    from(components.getByName("java"))
                }
            }
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
