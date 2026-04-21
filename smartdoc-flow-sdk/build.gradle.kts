dependencies {
    api(project(":smartdoc-flow-core"))
    implementation(project(":smartdoc-flow-format"))
    implementation(project(":smartdoc-flow-ocr"))
    implementation(project(":smartdoc-flow-layout"))
    implementation(project(":smartdoc-flow-render"))

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
