package io.github.kmbisset89.plugin

sealed class ReleaseType{
    object Unreleased : ReleaseType()

    data class Released(val version : String) : ReleaseType()
}
