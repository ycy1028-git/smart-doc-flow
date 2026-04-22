plugins {
    application
}

dependencies {
    implementation(project(":smartdoc-flow-sdk"))

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("io.ycy.smartdocflow.cli.SmartDocFlowCliApplication")
}
