package com.cf.jkmp3gainwrapper

import com.cf.jkmp3gainwrapper.entity.AddGainChange
import com.cf.jkmp3gainwrapper.entity.ApplyGainChange
import com.cf.jkmp3gainwrapper.entity.RecommendedGainChange
import com.cf.jkmp3gainwrapper.entity.UndoMP3GainChange
import com.cf.jkmp3gainwrapper.exception.MP3GainException
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader

class MP3Gain @JvmOverloads constructor(private var mp3gainPath: String,
                                        private var targetDB: Int = 89,
                                        private var preserveTimestamp: Boolean = true) {

    companion object {
        private const val MAX_FILES = 15
        private val logger = LoggerFactory.getLogger(this.javaClass)
    }

    /**
     * Gets the Version as a String e.g. 1.4.6
     *
     * @return the version as a String
     */
    @Throws(MP3GainException::class)
    fun getVersion(): String? {
        logger.debug("get Mp3gain version.")
        try {
            val cmd = mutableListOf(mp3gainPath, "-v")

            logger.debug("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            var result: String? = null
            BufferedReader(InputStreamReader(currentProcess.errorStream)).forEachLine {
                result = it.split(" ").last()
            }

            return result
        } catch (e: Exception) {
            logger.error("Error while getting version.", e)
            throw MP3GainException("Error while getting version.", e)
        }
    }

    /**
     * deletes the stored MP3Gain Tags (parameter: -s d)
     * ATTENTION: Only works with up to 15 files
     *
     * @param files the files which should be cleaned
     *
     * @return true if Tag is deleted, else false
     */
    @Throws(MP3GainException::class)
    fun deleteStoredTagInfo(files: List<String>): Boolean {
        logger.debug("Delete Stored Tag Info from: $files")
        try {
            if (files.size > MAX_FILES)
                throw IllegalArgumentException("Can't process more than 15 files. Given: ${files.size}")

            val cmd = mutableListOf(mp3gainPath, "-s", "d")
            if (preserveTimestamp)
                cmd.add("-p")
            cmd.addAll(files)

            logger.debug("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            currentProcess.waitFor()

            return true
        } catch (e: Exception) {
            logger.error("Error while deleting stored Tag Info from: $files", e)
            throw MP3GainException("Error while deleting stored Tag Info from: $files", e)
        }
    }

    /**
     * Undo the changes for the given Files (parameter: -u)
     * ATTENTION: Only works with up to 15 files
     *
     * @param files the files to parse
     * @param thread a thread for example for a GUI which gets the input stream and is started
     *
     * @return a list with the files and made changes. If changes are made it contains information about the changed track and album Gain Changes
     */
    @JvmOverloads
    @Throws(MP3GainException::class)
    fun undoMp3gainChanges(files: List<String>, thread: MP3GainThread? = null): List<UndoMP3GainChange> {
        logger.debug("Undoing mp3gain changes from: $files")
        try {
            if (files.size > MAX_FILES)
                throw IllegalArgumentException("Can't process more than 15 files. Given: ${files.size}")

            val cmd = mutableListOf(mp3gainPath, "-u", "-o")
            if (preserveTimestamp)
                cmd.add("-p")
            cmd.addAll(files)

            logger.debug("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            // if error stream is not used this will not work
            if (thread != null) {
                thread.setInputStream(currentProcess.errorStream)
                thread.start()
            } else {
                BufferedReader(InputStreamReader(currentProcess.errorStream)).forEachLine {
                    if (it.isNotBlank()) logger.debug(it)
                }
            }

            val result = mutableListOf<UndoMP3GainChange>()
            BufferedReader(InputStreamReader(currentProcess.inputStream)).forEachLine {
                val entries = it.split("\t")
                if (entries.size != 3 || entries[0] == "File")
                    return@forEachLine

                result.add(UndoMP3GainChange(entries[0], entries[1].toInt(), entries[2].toInt()))
            }

            return result
        } catch (e: Exception) {
            logger.error("Error while undoing mp3gain changes from: $files", e)
            throw MP3GainException("Error while undoing mp3gain changes from: $files", e)
        }
    }

    /**
     * applies the track gain so that it will be normalized to default gain (parameter: -r -o [-p] [-k] [-d x])
     * ATTENTION: Only works with up to 15 files
     *
     * @param files the files to modify
     * @param untilNoClipping if true, the targetDB will be ignored and the volume will be lowered until no clipping occures
     * @param thread a thread for example for a GUI which gets the input stream and is started
     *
     * @return a list of changes applied
     */
    @JvmOverloads
    @Throws(MP3GainException::class)
    fun applyTrackGain(files: List<String>, untilNoClipping: Boolean = false, thread: MP3GainThread? = null): List<ApplyGainChange> {
        logger.debug("Apply Track gain to: $files; until no clipping: $untilNoClipping, targetDB: $targetDB")
        try {
            if (files.size > MAX_FILES)
                throw IllegalArgumentException("Can't process more than 15 files. Given: ${files.size}")

            val cmd = mutableListOf(mp3gainPath, "-r", "-o", "-c")
            if (preserveTimestamp)
                cmd.add("-p")
            if (untilNoClipping)
                cmd.add("-k")
            if (!untilNoClipping && targetDB != 89)
                cmd.addAll(listOf("-d", (targetDB - 89).toString()))
            cmd.addAll(files)

            logger.debug("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            // if error stream is not used this will not work
            if (thread != null) {
                thread.setInputStream(currentProcess.errorStream)
                thread.start()
            } else {
                BufferedReader(InputStreamReader(currentProcess.errorStream)).forEachLine {
                    if (it.isNotBlank()) logger.debug(it)
                }
            }

            val result = mutableListOf<ApplyGainChange>()
            BufferedReader(InputStreamReader(currentProcess.inputStream)).forEachLine {
                val entries = it.split("\t")
                if (entries.size != 6 || entries[0] == "File")
                    return@forEachLine

                result.add(ApplyGainChange(entries[0], entries[1].toInt(), entries[2].toDouble(), entries[3].toDouble(), entries[4].toInt(), entries[5].toInt()))
            }

            return result
        } catch (e: Exception) {
            logger.error("Error while applying track gain changes from: $files", e)
            throw MP3GainException("Error while applying track gain changes from: $files", e)
        }
    }

    /**
     * applies the album gain so that it will be normalized to default gain (parameter: -a -o [-p] [-k] [-d x])
     * ATTENTION: Only works with up to 15 files
     *
     * @param files the files to modify
     * @param untilNoClipping if true, the targetDB will be ignored and the volume will be lowered until no clipping occures
     * @param thread a thread for example for a GUI which gets the input stream and is started
     *
     * @return a list of changes applied
     */
    @JvmOverloads
    @Throws(MP3GainException::class)
    fun applyAlbumGain(files: List<String>, untilNoClipping: Boolean = false, thread: MP3GainThread? = null): List<ApplyGainChange> {
        logger.debug("Apply Album gain to: $files; until no clipping: $untilNoClipping, targetDB: $targetDB")
        try {
            if (files.size > MAX_FILES)
                throw IllegalArgumentException("Can't process more than 15 files. Given: ${files.size}")

            val cmd = mutableListOf(mp3gainPath, "-a", "-o", "-c")
            if (preserveTimestamp)
                cmd.add("-p")
            if (untilNoClipping)
                cmd.add("-k")
            if (!untilNoClipping && targetDB != 89)
                cmd.addAll(listOf("-d", (targetDB - 89).toString()))
            cmd.addAll(files)

            logger.debug("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            // if error stream is not used this will not work
            if (thread != null) {
                thread.setInputStream(currentProcess.errorStream)
                thread.start()
            } else {
                BufferedReader(InputStreamReader(currentProcess.errorStream)).forEachLine {
                    if (it.isNotBlank()) logger.debug(it)
                }
            }

            val result = mutableListOf<ApplyGainChange>()
            BufferedReader(InputStreamReader(currentProcess.inputStream)).forEachLine {
                val entries = it.split("\t")
                if (entries.size != 6 || entries[0] == "File")
                    return@forEachLine

                result.add(ApplyGainChange(entries[0], entries[1].toInt(), entries[2].toDouble(), entries[3].toDouble(), entries[4].toInt(), entries[5].toInt()))
            }

            return result
        } catch (e: Exception) {
            logger.error("Error while applying album gain changes from: $files", e)
            throw MP3GainException("Error while applying album gain changes from: $files", e)
        }
    }

    /**
     * reads the tag info for the given files. Containing DB and Gain Changes for Track and Album (parameter: -s r)
     * ATTENTION: Only works with up to 15 files
     *
     * @param files the files to parse
     * @param thread a thread for example for a GUI which gets the input stream and is started
     *
     * @return a list with the recommended changes for each file
     */
    @JvmOverloads
    @Throws(MP3GainException::class)
    fun analyzeGain(files: List<String>, thread: MP3GainThread? = null): List<RecommendedGainChange> {
        logger.debug("Apply Album gain to: $files; targetDB: $targetDB")
        try {
            if (files.size > MAX_FILES)
                throw IllegalArgumentException("Can't process more than 15 files. Given: ${files.size}")

            val cmd = mutableListOf(mp3gainPath, "-s", "r", "-o")
            if (preserveTimestamp)
                cmd.add("-p")
            if (targetDB != 89)
                cmd.addAll(listOf("-d", (targetDB - 89).toString()))
            cmd.addAll(files)

            logger.debug("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            // if error stream is not used this will not work
            if (thread != null) {
                thread.setInputStream(currentProcess.errorStream)
                thread.start()
            } else {
                BufferedReader(InputStreamReader(currentProcess.errorStream)).forEachLine {
                    if (it.isNotBlank()) logger.debug(it)
                }
            }

            val result = mutableListOf<RecommendedGainChange>()
            var albumChanges: RecommendedGainChange? = null
            BufferedReader(InputStreamReader(currentProcess.inputStream)).forEachLine {
                val entries = it.split("\t")
                if (entries.size != 6 || entries[0] == "File")
                    return@forEachLine
                if (entries[0] == "\"Album\"") {
                    albumChanges = RecommendedGainChange(entries[0], entries[1].toInt(), entries[2].toDouble(), entries[3].toDouble(), entries[4].toInt(),
                            entries[5].toInt())
                    return@forEachLine
                }

                result.add(RecommendedGainChange(entries[0], entries[1].toInt(), entries[2].toDouble(), entries[3].toDouble(), entries[4].toInt(),
                        entries[5].toInt()))
            }

            // apply album changes
            result.forEach { it.albumChanges = albumChanges }

            return result
        } catch (e: Exception) {
            logger.error("Error while applying album gain changes from: $files", e)
            throw MP3GainException("Error while applying album gain changes from: $files", e)
        }
    }

    /**
     * adds a given gain value to the file. 1 gain = 1.5 DB (parameter: -g x)
     * ATTENTION: Only works with up to 15 files
     *
     * @param files the files to modify
     * @param gain the gain to add to each file
     * @param thread a thread for example for a GUI which gets the input stream and is started
     *
     * @return a list of changes which are applied
     */
    @JvmOverloads
    @Throws(MP3GainException::class)
    fun addGain(files: List<String>, gain: Int, thread: MP3GainThread? = null): List<AddGainChange> {
        logger.debug("Add $gain gain to: $files")
        try {
            if (files.size > MAX_FILES)
                throw IllegalArgumentException("Can't process more than 15 files. Given: ${files.size}")

            val cmd = mutableListOf(mp3gainPath, "-g", gain.toString())
            if (preserveTimestamp)
                cmd.add("-p")
            cmd.addAll(files)

            logger.debug("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            // if error stream is not used this will not work
            if (thread != null) {
                thread.setInputStream(currentProcess.errorStream)
                thread.start()
            } else {
                BufferedReader(InputStreamReader(currentProcess.errorStream)).forEachLine {
                    if (it.isNotBlank()) logger.debug(it)
                }
            }

            return files.map { AddGainChange(it, gain) }
        } catch (e: Exception) {
            logger.error("Error while adding gain to: $files", e)
            throw MP3GainException("Error while adding gain to: $files", e)
        }
    }
}