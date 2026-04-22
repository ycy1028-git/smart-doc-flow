plugins {
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    java
}

dependencies {
    implementation(project(":smartdoc-flow-sdk"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.apache.poi:poi-ooxml:5.2.5")
    testImplementation("org.apache.pdfbox:pdfbox:3.0.2")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
