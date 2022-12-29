package io.github.kmbisset89.changelog.plugin

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

const val DEFAULT_OUTPUT_FILE = "CHANGELOG.md"

@Suppress("UnnecessaryAbstractClass")
abstract class ChangeLogExtension @Inject constructor(project: Project) {

    private val objects = project.objects

    val jsonChangeLogFormatFilePath: Property<String> = objects.property(String::class.java)

    // Example of a property that is optional.
    val gitFilePath: Property<String> = objects.property(String::class.java)
    val regexForSemVerTag: Property<String> = objects.property(String::class.java)

    // Example of a property with a default set with .convention
    val outputFile: RegularFileProperty = objects.fileProperty().convention(
        project.layout.buildDirectory.file(DEFAULT_OUTPUT_FILE)
    )
}
