dependencies {
    api(project(":smartdoc-flow-core"))
    implementation(project(":smartdoc-flow-format"))
    implementation(project(":smartdoc-flow-ocr"))
    implementation(project(":smartdoc-flow-layout"))
    implementation(project(":smartdoc-flow-render"))

    testImplementation("org.apache.poi:poi-ooxml:5.2.5")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":smartdoc-flow-common"))
}

sourceSets {
    test {
        resources.srcDir(project(":smartdoc-flow-format").file("src/test/resources"))
    }
}
