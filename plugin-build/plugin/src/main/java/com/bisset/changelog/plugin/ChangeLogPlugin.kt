package com.bisset.changelog.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

const val EXTENSION_NAME = "changeLogConfig"
const val TASK_NAME = "makeChangeLog"

abstract class ChangeLogPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Add the 'template' extension object
        val extension = project.extensions.create(EXTENSION_NAME, ChangeLogExtension::class.java, project)

        // Add a task that uses configuration from the extension object
        project.tasks.register(TASK_NAME, ChangeLogTask::class.java) {
            it.jsonChangeLogFormatFile.set(extension.jsonChangeLogFormatFilePath)
            it.regexForSemVerTag.set(extension.regexForSemVerTag)
            it.gitFilePath.set(extension.gitFilePath)
            it.mainBranch.set(extension.mainBranch)
            it.outputFile.set(extension.outputFile)
        }
    }
}
