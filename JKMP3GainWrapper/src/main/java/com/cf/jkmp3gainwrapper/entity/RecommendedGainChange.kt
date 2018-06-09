package com.cf.jkmp3gainwrapper.entity

data class RecommendedGainChange(var filePath: String,
                                 var trackMp3Gain: Int,
                                 var trackDbGain: Double,
                                 var maxAmplitude: Double,
                                 var maxGlobalGain: Int,
                                 var minGlobalGain: Int,
                                 var albumChanges: RecommendedGainChange? = null) {

    fun hasClipping(): Boolean {
        // if > 31000 -> clipping
        return maxAmplitude > 31000
    }
}