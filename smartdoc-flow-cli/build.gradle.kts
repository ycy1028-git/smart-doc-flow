plugins {
    application
}

dependencies {
    implementation(project(":smartdoc-flow-sdk"))
    implementation("org.apache.pdfbox:pdfbox:3.0.2")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("io.ycy.smartdocflow.cli.SmartDocFlowCliApplication")
}

tasks.register<JavaExec>("generateBenchmarkSamples") {
    group = "benchmark"
    description = "Generates benchmark sample files"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.ycy.smartdocflow.cli.BenchmarkSampleGenerator")
    args(project.rootDir.resolve("benchmarks/generated").absolutePath)
    args(project.rootDir.absolutePath)
}
