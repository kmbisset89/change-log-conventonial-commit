import java.io.FileInputStream
import java.util.Properties

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `java-library`
    alias(libs.plugins.pluginPublish)
}

group = "com.bisset.changelog"
dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation("org.json:json:20220924")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r")
    implementation("net.steppschuh.markdowngenerator:markdowngenerator:1.3.1.1")

    testImplementation(libs.junit)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    plugins {
        create(property("ID").toString()) {
            id = property("ID").toString()
            implementationClass = property("IMPLEMENTATION_CLASS").toString()
            version = property("VERSION").toString()
            displayName = property("DISPLAY_NAME").toString()
        }
    }
}

// Configuration Block for the Plugin Marker artifact on Plugin Central
pluginBundle {
    website = property("WEBSITE").toString()
    vcsUrl = property("VCS_URL").toString()
    description = property("DESCRIPTION").toString()
    tags = listOf( "change log", "conventional commit")
}

tasks.create("setupPluginUploadFromEnvironment") {
    doLast {
        val localProperties = Properties()
        localProperties.load(FileInputStream(rootProject.file("local.properties")))

        val key = localProperties["gradle.publish.key"] as? String
        val secret = localProperties["gradle.publish.secret"] as? String

        if (key == null || secret == null) {
            throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
        }

        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}
