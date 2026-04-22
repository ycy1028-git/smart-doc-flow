dependencies {
    api(project(":smartdoc-flow-core"))
    implementation("org.apache.pdfbox:pdfbox:3.0.2")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
