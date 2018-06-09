package com.cf.jkmp3gainwrapper.entity

data class UndoMP3GainChange(var filePath: String,
                             var leftGlobalGainChange: Int = 0,
                             var rightGlobalGainChange: Int = 0) {

    fun hasNoChanges(): Boolean {
        return leftGlobalGainChange == 0 && rightGlobalGainChange == 0
    }
}