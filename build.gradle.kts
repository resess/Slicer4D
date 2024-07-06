plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.intellij") version "1.13.0"
}

group = "anonymous"
version = "0.1.1"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")}
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.2.3")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("java", "org.jetbrains.java.decompiler"))
}

dependencies {
    implementation(project(":vendor:slicer4j:Slicer4J", "default"))
    implementation("org.testng:testng:7.1.0")

    testImplementation("com.intellij.remoterobot:remote-robot:0.11.18")
    testImplementation ("com.intellij.remoterobot:remote-fixtures:0.11.18")
    testImplementation ("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation ("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly ("org.junit.platform:junit-platform-launcher:1.9.2")

    // Logging Network Calls
    testImplementation ("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Video Recording
    implementation ("com.automation-remarks:video-recorder-junit5:2.0")
    // JavaSlicer
    implementation(files("libs/sdg-cli-1.3.0-jar-with-dependencies.jar"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    named<Jar>("jar") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("223.*")
    }

    downloadRobotServerPlugin {
        version.set("0.11.18")
    }

    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("ide.mac.file.chooser.native", "false")
        systemProperty("apple.laf.useScreenMenuBar", "false")
        systemProperty("idea.trust.all.projects", "true")
        systemProperty("ide.show.tips.on.startup.default.value", "false")
        systemProperty("jbScreenMenuBar.enabled", "false")
        systemProperty("idea.trust.all.projects", "true")
        systemProperty("ide.show.tips.on.startup.default.value", "false")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    test {
        exclude("**/pages/**")
    }
}
