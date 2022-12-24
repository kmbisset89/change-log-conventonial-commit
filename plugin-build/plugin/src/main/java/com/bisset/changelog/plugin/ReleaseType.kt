package com.bisset.changelog.plugin

sealed class ReleaseType{
    object Unreleased : ReleaseType()

    data class Released(val version : String) : ReleaseType()
}
