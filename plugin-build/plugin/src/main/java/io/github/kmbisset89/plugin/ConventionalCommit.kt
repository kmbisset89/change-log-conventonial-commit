package io.github.kmbisset89.plugin

sealed class ConventionalCommit {
    abstract val tags: Set<String>
    abstract val description: String
    abstract val body: String?
    abstract val footers: Map<String, String>
    abstract val timeOfCommit : Int

    data class Feature(
        override val tags: Set<String>,
        override val description: String,
        override val body: String? = null,
        override val footers: Map<String, String> = mapOf(),
        override val timeOfCommit: Int
    ) : ConventionalCommit()

    data class Fix(
        override val tags: Set<String>,
        override val description: String,
        override val body: String? = null,
        override val footers: Map<String, String> = mapOf(),
        override val timeOfCommit: Int
    ) : ConventionalCommit()

    data class Changes(
        override val tags: Set<String>,
        override val description: String,
        override val body: String? = null,
        override val footers: Map<String, String> = mapOf(),
        override val timeOfCommit: Int
    ) : ConventionalCommit()
}
