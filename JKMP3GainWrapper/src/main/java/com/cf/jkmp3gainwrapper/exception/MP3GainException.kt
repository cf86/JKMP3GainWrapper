package com.cf.jkmp3gainwrapper.exception

class MP3GainException : java.lang.Exception {

    constructor(message: String, ex: Exception?) : super(message, ex)
    constructor(message: String) : super(message)
    constructor(ex: Exception) : super(ex)
}