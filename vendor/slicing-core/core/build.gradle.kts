plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("guru.nidi:graphviz-java:0.18.1")
    implementation("de.fraunhofer.sit.sse.flowdroid:soot-infoflow:2.10.0")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.json:json:20201115")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("org.jgrapht:jgrapht-core:1.2.0")
    implementation("commons-cli:commons-cli:1.4")
}
