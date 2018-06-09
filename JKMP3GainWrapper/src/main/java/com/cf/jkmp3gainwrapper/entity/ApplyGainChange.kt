package com.cf.jkmp3gainwrapper.entity

data class ApplyGainChange(var filePath: String,
                           var mp3Gain: Int,
                           var dbGain: Double,
                           var maxAmplitude: Double,
                           var maxGlobalGain: Int,
                           var minGlobalGain: Int) {

    fun hasClipping(): Boolean {
        // if > 31000 -> clipping
        return maxAmplitude > 31000
    }
}