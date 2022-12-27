package io.github.kmbisset89.plugin

import net.steppschuh.markdowngenerator.list.UnorderedList
import net.steppschuh.markdowngenerator.rule.HorizontalRule
import net.steppschuh.markdowngenerator.text.emphasis.BoldText
import net.steppschuh.markdowngenerator.text.heading.Heading
import org.json.JSONObject

private const val DESCRIPTION = "description"

class MarkDownMarkerFactory(
    private val releaseToCommit: Map<ReleaseType, List<ConventionalCommit>>,
    private val jsonFormat: JSONObject,
) {
    companion object {
        private const val ATTR = "attr"
        private const val TEXT = "text"
        private const val BREAK_AFTER = "breakAfter"

        private const val HEADING_LEVEL_1 = "H1"
        private const val HEADING_LEVEL_2 = "H2"
        private const val HEADING_LEVEL_3 = "H3"
        private const val BOLD = "bold"
        private const val BULLET = "bullet"
        private const val PARAGRAPH = "para"
        private const val NEW_LINE = "newLine"
        private const val HORIZONTAL_RULE = "horizontalRule"
        private const val TAG = "tag"
    }

    fun make(): String {
        return buildString {
            if (jsonFormat.has("title")) {
                handleTitle()
            } else {
                inputDefaultTitle()
            }

            if (jsonFormat.has("introduction")) {
                handleIntroduction()
            }

            jsonFormat.optJSONObject("eachVersion")?.let {
                handleEachCommit(it)
            }
        }
    }


    private fun StringBuilder.handleEachCommit(eachCommitJson: JSONObject) {
        handleUnreleased(eachCommitJson)
        handleReleased(eachCommitJson)
    }

    private fun StringBuilder.handleReleased(eachCommitJson: JSONObject) {
        val releases =
            releaseToCommit.mapNotNull {
                if (it.key is ReleaseType.Released) {
                    it.key as ReleaseType.Released to it.value
                } else {
                    null
                }
            }.toMap()
        if (releases.isNullOrEmpty()) {
            // Skip this section for now
        } else {
            releases.forEach { (release, list) ->
                if (eachCommitJson.has(TAG)) {
                    val tagObj = eachCommitJson.optJSONObject(TAG)
                    val attr = tagObj.optString(ATTR)
                    inputText(release.version, this, attr)
                }
                val featureList = list.filterIsInstance<ConventionalCommit.Feature>().sortedBy { it.timeOfCommit }
                val bugfixList = list.filterIsInstance<ConventionalCommit.Fix>().sortedBy { it.timeOfCommit }
                val changeList = list.filterIsInstance<ConventionalCommit.Changes>().sortedBy { it.timeOfCommit }


                handleConventionalCommitInfo(eachCommitJson, featureList, bugfixList, changeList)
                inputText("", this, NEW_LINE)
                inputText("", this, HORIZONTAL_RULE)
            }
        }
    }


    private fun StringBuilder.handleUnreleased(eachCommitJson: JSONObject) {
        val unreleasedInfo = releaseToCommit[ReleaseType.Unreleased]
        if (unreleasedInfo.isNullOrEmpty()) {
            // Skip this section for now
        } else {
            val featureList = unreleasedInfo.filterIsInstance<ConventionalCommit.Feature>().sortedBy { it.timeOfCommit }
            val bugfixList = unreleasedInfo.filterIsInstance<ConventionalCommit.Fix>().sortedBy { it.timeOfCommit }
            val changeList = unreleasedInfo.filterIsInstance<ConventionalCommit.Changes>().sortedBy { it.timeOfCommit }

            if (eachCommitJson.has(TAG)) {
                val tagObj = eachCommitJson.optJSONObject(TAG)
                val attr = tagObj.optString(ATTR)
                inputText("Unreleased Changes", this, attr)
            }

            handleConventionalCommitInfo(eachCommitJson, featureList, bugfixList, changeList)
            inputText("", this, NEW_LINE)
            inputText("", this, HORIZONTAL_RULE)
        }
    }

    private fun StringBuilder.handleConventionalCommitInfo(
        eachCommitJson: JSONObject,
        featureList: List<ConventionalCommit.Feature>,
        bugfixList: List<ConventionalCommit.Fix>,
        changeList: List<ConventionalCommit.Changes>
    ) {
        eachCommitJson.optJSONObject("feat")?.let { featObj ->
            val titleObject = featObj.optJSONObject("title")
            val attr = titleObject?.optString(ATTR)
            val text = titleObject?.optString(TEXT)
            val breakAfter = titleObject?.optString(BREAK_AFTER)
            text?.let { titleText ->
                inputText(titleText, this, attr)
                breakAfter?.let { type ->
                    inputText("", this, type)
                }
            }

            if (featureList.isNotEmpty()) {
                handleType(featObj, featureList)
            } else {
                inputText("No new features added.", this, null)
            }
        }

        eachCommitJson.optJSONObject("fix")?.let { fixObj ->
            val titleObject = fixObj.optJSONObject("title")
            val attr = titleObject?.optString(ATTR)
            val text = titleObject?.optString(TEXT)
            val breakAfter = titleObject?.optString(BREAK_AFTER)
            text?.let { titleText ->
                inputText(titleText, this, attr)
                breakAfter?.let { type ->
                    inputText("", this, type)
                }
            }

            if (bugfixList.isNotEmpty()) {
                handleType(fixObj, bugfixList)
            } else {
                inputText("No new bugs addressed.", this, null)
            }
        }

        eachCommitJson.optJSONObject("change")?.let { changeObj ->
            val titleObject = changeObj.optJSONObject("title")
            val attr = titleObject?.optString(ATTR)
            val text = titleObject?.optString(TEXT)
            val breakAfter = titleObject?.optString(BREAK_AFTER)
            text?.let { titleText ->
                inputText(titleText, this, attr)
                breakAfter?.let { type ->
                    inputText("", this, type)
                }
            }

            if (changeList.isNotEmpty()) {
                handleType(changeObj, changeList)
            } else {
                inputText("No existing features changed.", this, null)
            }
        }
    }

    private fun StringBuilder.handleType(
        typeObj: JSONObject,
        featureList: List<ConventionalCommit>
    ) {
        typeObj.optJSONObject("each")?.let { eachObj ->
            featureList.forEach { cc ->
                eachObj.optJSONObject(DESCRIPTION)?.let { descObj ->
                    val attr = descObj.optString(ATTR)
                    val breakAfter = descObj.optString(BREAK_AFTER)
                    inputText(cc.description, this, attr)
                    breakAfter?.let { type ->
                        inputText("", this, type)
                    }
                }
                eachObj.optJSONObject("body")?.let { bodyObj ->
                    cc.body?.let { bodyText ->
                        val attr = bodyObj.optString(ATTR)
                        val breakAfter = bodyObj.optString(BREAK_AFTER)
                        if(bodyText.isNotEmpty()) {
                            inputText(bodyText, this, attr)
                        }
                        breakAfter?.let { type ->
                            inputText("", this, type)
                        }
                    }
                }

                eachObj.optJSONObject("footer")?.let { footerObj ->
                    val attr = footerObj.optString(ATTR)
                    val breakAfter = footerObj.optString(BREAK_AFTER)
                    cc.footers.forEach { (key, value) ->
                        inputText("$key : $value", this, attr)
                        appendLine()
                    }
                    if (cc.footers.isNotEmpty()) {
                        breakAfter?.let { type ->
                            inputText("", this, type)
                        }
                    }
                }
            }
        }
    }

    private fun StringBuilder.handleTitle() {
        val titleObject = jsonFormat.optJSONObject("title")
        val attr = titleObject?.optString(ATTR)
        val text = titleObject?.optString(TEXT)
        val breakAfter = titleObject?.optString(BREAK_AFTER)
        text?.let { titleText ->
            inputText(titleText, this, attr)
            breakAfter?.let { type ->
                inputText("", this, type)
            }
        } ?: run {
            inputDefaultTitle()
        }
    }

    private fun StringBuilder.inputDefaultTitle() {
        inputText("Change Log", this, HEADING_LEVEL_1)
        inputText("", this, HORIZONTAL_RULE)
    }

    private fun StringBuilder.handleIntroduction() {
        val introductionObj = jsonFormat.optJSONObject("introduction")
        val attr = introductionObj?.optString(ATTR)
        val text = introductionObj?.optString(TEXT)
        val breakAfter = introductionObj?.optString(BREAK_AFTER)
        text?.let { introText ->
            inputText(introText, this, attr)
            breakAfter?.let { type ->
                inputText("", this, type)
            }
        }
    }

    private fun inputText(string: String, builder: StringBuilder, attr: String?) {
        when (attr) {
            HEADING_LEVEL_1 -> {
                builder.appendLine(Heading(string, 1))
            }

            HEADING_LEVEL_2 -> {
                builder.appendLine(Heading(string, 2))
            }

            HEADING_LEVEL_3 -> {
                builder.appendLine(Heading(string, 3))
            }

            HORIZONTAL_RULE -> {
                builder.appendLine(HorizontalRule())
            }

            NEW_LINE -> {
                builder.appendLine()
            }

            BOLD -> {
                builder.appendLine(BoldText(string))
            }
            BULLET -> {
                builder.appendLine(UnorderedList(listOf(string)))
            }

            else -> {
                builder.appendLine(string)
            }
        }
    }
}
