package com.cf.jkmp3gainwrapper

import java.io.InputStream

abstract class MP3GainThread : Thread() {

    /**
     * sets the Mp3Gain error stream, this for example contains the % done messages
     */
    abstract fun setInputStream(ins: InputStream)
}