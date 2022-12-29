package io.github.kmbisset89.changelog.plugin

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevCommit
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

abstract class ChangeLogTask : DefaultTask() {
    private val logger = Logging.getLogger(ChangeLogTask::class.java)

    init {
        description = "Generates a change log based off of Git history and parses conventional commit style."

        // Don't forget to set the group here.
        // group = BasePlugin.BUILD_GROUP
    }

    private val defaultJson =
        "{\"title\":{\"attr\":\"H1\",\"text\":\"Change Log\",\"breakAfter\":\"horizontalRule\"},\"introduction\":{\"attr\":\"bold\",\"breakAfter\":\"newLine\"},\"eachVersion\":{\"tag\":{\"attr\":\"H2\",\"breakAfter\":\"newLine\"},\"feat\":{\"title\":{\"attr\":\"H3\",\"text\":\"Features Added\",\"breakAfter\":\"newLine\"},\"each\":{\"description\":{\"attr\":\"bold\",\"breakAfter\":\"newLine\"},\"body\":{\"attr\":\"bullet\",\"breakAfter\":\"newLine\"},\"footer\":{\"attr\":\"para\",\"breakAfter\":\"newLine\"}}},\"fix\":{\"title\":{\"attr\":\"H3\",\"text\":\"Bugs Addressed\",\"breakAfter\":\"newLine\"},\"each\":{\"description\":{\"attr\":\"bold\",\"breakAfter\":\"newLine\"},\"body\":{\"attr\":\"bullet\",\"breakAfter\":\"newLine\"},\"footer\":{\"attr\":\"para\",\"breakAfter\":\"newLine\"}}},\"change\":{\"title\":{\"attr\":\"H3\",\"text\":\"Existing Feature Modifications\",\"breakAfter\":\"newLine\"},\"each\":{\"description\":{\"attr\":\"bold\",\"breakAfter\":\"newLine\"},\"body\":{\"attr\":\"bullet\",\"breakAfter\":\"newLine\"},\"footer\":{\"attr\":\"para\",\"breakAfter\":\"newLine\"}}}}}"

    @get:Input
    @get:Option(option = "regexForSemVerTag", description = "Regex to find commits between tags")
    @get:Optional
    abstract val regexForSemVerTag: Property<String>

    @get:Input
    @get:Option(
        option = "jsonChangeLogFormatFilePath",
        description = "The path to the JSON file that contains the formatting"
    )
    @get:Optional
    abstract val jsonChangeLogFormatFile: Property<String>

    @get:Input
    @get:Option(option = "gitFilePath", description = "Git File path")
    @get:Optional
    abstract val gitFilePath: Property<String>


    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun makeChangeLog() {
        val regex = Regex(regexForSemVerTag.orNull ?: "^v\\d{1,20}.\\d{1,20}.\\d{1,20}\$")
        var json = JSONObject(defaultJson)

        jsonChangeLogFormatFile.orNull?.let {
            val bufferedReader: BufferedReader = File(it).bufferedReader()
            val inputString = bufferedReader.use { text -> text.readText() }
            json = JSONObject(inputString)
        }

        val gitRepo = FileRepository(
            when{
                project.hasProperty("git.root") -> {
                    logger.info("Found git.root property")
                    project.property("git.root") as String
                }
                gitFilePath.orNull != null -> {
                    logger.info("Found git.root property")
                    gitFilePath.get()
                }
                else -> {
                    logger.info("Using project path as git root")
                    project.rootProject.projectDir.path
                }
            }
        )
        val git = Git(gitRepo)
        val mapOfCommitToTags = LinkedHashMap<Commit, MutableSet<Tag>>()
        gitRepo.refDatabase.getRefsByPrefix(Constants.R_TAGS).forEach {
            val peeledRef = gitRepo.peel(it)
            val shortTag = it.name.substring(10, it.name.length)
            val commit = peeledRef.peeledObjectId?.name ?: peeledRef.objectId.name
            val tags = mapOfCommitToTags[Commit(commit)] ?: HashSet()
            tags.add(Tag(shortTag))
            mapOfCommitToTags[Commit(commit)] = tags
        }

        val listOfConventionalCommits = git.log().call().mapNotNull {
            createConventionalCommit(it, mapOfCommitToTags)
        }

        val releaseToCommits: Map<ReleaseType, List<ConventionalCommit>> = makeMap(regex, listOfConventionalCommits)

        outputFile.get().asFile.writeText(
            MarkDownMarkerFactory(
                releaseToCommits,
                json
            ).make()
        )
    }

    private fun makeMap(
        regex: Regex,
        listOfConventionalCommits: List<ConventionalCommit>
    ): Map<ReleaseType, List<ConventionalCommit>> {
        val tempMap = LinkedHashMap<ReleaseType, MutableList<ConventionalCommit>>()
        var currentReleaseType: ReleaseType = ReleaseType.Unreleased

        listOfConventionalCommits.forEach { cc ->
            val version = cc.tags.find { it.matches(regex) }
            val releaseType = if (version != null) {
                val finalRelease = ReleaseType.Released(version)
                currentReleaseType = finalRelease
                finalRelease
            } else {
                if (currentReleaseType is ReleaseType.Released) {
                    currentReleaseType
                } else {
                    ReleaseType.Unreleased
                }
            }
            val listOfComments = tempMap[releaseType] ?: ArrayList()
            listOfComments.add(cc)
            tempMap[releaseType] = listOfComments
        }

        return tempMap
    }

    private fun createConventionalCommit(
        commit: RevCommit?,
        mapOfCommitToTags: Map<Commit, Set<Tag>>
    ): ConventionalCommit? {
        return commit?.let { c ->
            mapOfCommitToTags[Commit(c.name)]?.let { tag ->
                val fullMessage = c.fullMessage
                when {
                    fullMessage.startsWith("feat") -> {
                        val brokenMessage = breakApartMessage(c.fullMessage.lines())
                        ConventionalCommit.Feature(
                            tags = tag.map { it.tag }.toSet(),
                            description = brokenMessage.first.trim(),
                            body = brokenMessage.second,
                            footers = brokenMessage.third,
                            timeOfCommit = c.commitTime
                        )
                    }

                    fullMessage.startsWith("fix") -> {
                        val brokenMessage = breakApartMessage(c.fullMessage.lines())
                        ConventionalCommit.Fix(
                            tags = tag.map { it.tag }.toSet(),
                            description = brokenMessage.first.trim(),
                            body = brokenMessage.second,
                            footers = brokenMessage.third,
                            timeOfCommit = c.commitTime

                        )
                    }

                    fullMessage.startsWith("change") -> {
                        val brokenMessage = breakApartMessage(c.fullMessage.lines())
                        ConventionalCommit.Changes(
                            tags = tag.map { it.tag }.toSet(),
                            description = brokenMessage.first.trim(),
                            body = brokenMessage.second,
                            footers = brokenMessage.third,
                            timeOfCommit = c.commitTime
                        )
                    }

                    else -> null
                }
            }
        }
    }

    private fun breakApartMessage(brokenMessage: List<String>): Triple<String, String?, Map<String, String>> {
        val isAtBody = AtomicBoolean(false)
        val isAtFooter = AtomicBoolean(false)
        val desc = brokenMessage.firstOrNull()?.let { first ->
            first.substringAfterLast(":")
        } ?: "No description provided."
        val tmpMap = HashMap<String, String>()
        var body: String? = null

        for (i in brokenMessage.indices) {
            val stringRef = brokenMessage[i]
            if (i == 0) {
                continue
            }
            determineState(stringRef, isAtFooter, isAtBody, brokenMessage, i)
            if (isAtBody.get()) {
                if (body == null) {
                    body = stringRef
                } else {
                    body += stringRef
                }
            }
            if (isAtFooter.get()) {
                val split = stringRef.split(":")
                if (split.size == 2) {
                    tmpMap[split[0]] = split[1]
                } else {
                    // Weird footer
                }
            }
        }

        return Triple(desc, body, tmpMap)
    }

    private fun determineState(
        stringRef: String,
        isAtFooter: AtomicBoolean,
        isAtBody: AtomicBoolean,
        brokenMessage: List<String>,
        i: Int
    ) {

        if (stringRef.isNullOrBlank()) {
            when {
                isAtFooter.get() -> {
                    // Ignore the rest
                }

                !isAtBody.get() -> isAtBody.set(true)
                isAtBody.get() -> {
                    brokenMessage.getOrNull(i + 1)?.let {
                        val hasColon = it.contains(":")
                        val hasHashTag = it.contains("#")
                        when {
                            hasColon -> {
                                val indexOfColon = it.indexOf(":")
                                if (indexOfColon != it.length) {
                                    isAtFooter.set(true)
                                }
                            }

                            hasHashTag -> {
                                val indexOfHashTag = it.indexOf("#")
                                if (indexOfHashTag != it.length) {
                                    isAtFooter.set(true)
                                }
                            }

                            else -> {
                                // We are probably in a multi paragraph body
                            }
                        }
                    }
                }
            }
        }
    }

    private data class Commit(val commitHash: String)
    private data class Tag(val tag: String)

}
