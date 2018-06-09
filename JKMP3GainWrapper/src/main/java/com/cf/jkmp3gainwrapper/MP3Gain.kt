package com.cf.jkmp3gainwrapper

import com.cf.jkmp3gainwrapper.entity.AddGainChange
import com.cf.jkmp3gainwrapper.entity.ApplyGainChange
import com.cf.jkmp3gainwrapper.entity.RecommendedGainChange
import com.cf.jkmp3gainwrapper.entity.UndoMP3GainChange
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader

class MP3Gain(var mp3gainPath: String,
              var targetDB: Int = 89,
              var preserveTimestamp: Boolean = true) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    companion object {
        private const val MAX_FILES = 15
    }

    /**
     * deletes the stored MP3Gain Tags (parameter: -s d)
     * ATTENTION: Only works with up to 15 files
     *
     * @param files the files which should be cleaned
     *
     * @return true if Tag is deleted, else false
     */
    fun deleteStoredTagInfo(files: List<String>): Boolean {
        logger.info("Delete Stored Tag Info from: $files")
        try {
            if (files.size > MAX_FILES)
                throw IllegalArgumentException("Can't process more than 15 files. Given: ${files.size}")

            val cmd = mutableListOf(mp3gainPath, "-s", "d")
            if (preserveTimestamp)
                cmd.add("-p")
            cmd.addAll(files)

            logger.info("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            currentProcess.waitFor()

            return true
        } catch (e: Exception) {
            logger.error("Error while deleting stored Tag Info from: $files", e)
            return false
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
    fun undoMp3gainChanges(files: List<String>, thread: MP3GainThread? = null): List<UndoMP3GainChange> {
        logger.info("Undoing mp3gain changes from: $files")
        try {
            if (files.size > MAX_FILES)
                throw IllegalArgumentException("Can't process more than 15 files. Given: ${files.size}")

            val cmd = mutableListOf(mp3gainPath, "-u", "-o")
            if (preserveTimestamp)
                cmd.add("-p")
            cmd.addAll(files)

            logger.info("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            // if error stream is not used this will not work
            if (thread != null) {
                thread.setInputStream(currentProcess.errorStream)
                thread.start()
            } else {
                BufferedReader(InputStreamReader(currentProcess.errorStream)).forEachLine {
                    if (it.isNotBlank()) logger.info(it)
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
            return listOf()
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
    fun applyTrackGain(files: List<String>, untilNoClipping: Boolean = false, thread: MP3GainThread? = null): List<ApplyGainChange> {
        logger.info("Apply Track gain to: $files; until no clipping: $untilNoClipping, targetDB: $targetDB")
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

            logger.info("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            // if error stream is not used this will not work
            if (thread != null) {
                thread.setInputStream(currentProcess.errorStream)
                thread.start()
            } else {
                BufferedReader(InputStreamReader(currentProcess.errorStream)).forEachLine {
                    if (it.isNotBlank()) logger.info(it)
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
            return listOf()
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
    fun applyAlbumGain(files: List<String>, untilNoClipping: Boolean = false, thread: MP3GainThread? = null): List<ApplyGainChange> {
        logger.info("Apply Album gain to: $files; until no clipping: $untilNoClipping, targetDB: $targetDB")
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

            logger.info("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            // if error stream is not used this will not work
            if (thread != null) {
                thread.setInputStream(currentProcess.errorStream)
                thread.start()
            } else {
                BufferedReader(InputStreamReader(currentProcess.errorStream)).forEachLine {
                    if (it.isNotBlank()) logger.info(it)
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
            return listOf()
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
    fun analyzeGain(files: List<String>, thread: MP3GainThread? = null): List<RecommendedGainChange> {
        logger.info("Apply Album gain to: $files; targetDB: $targetDB")
        try {
            if (files.size > MAX_FILES)
                throw IllegalArgumentException("Can't process more than 15 files. Given: ${files.size}")

            val cmd = mutableListOf(mp3gainPath, "-s", "r", "-o")
            if (preserveTimestamp)
                cmd.add("-p")
            if (targetDB != 89)
                cmd.addAll(listOf("-d", (targetDB - 89).toString()))
            cmd.addAll(files)

            logger.info("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            // if error stream is not used this will not work
            if (thread != null) {
                thread.setInputStream(currentProcess.errorStream)
                thread.start()
            } else {
                BufferedReader(InputStreamReader(currentProcess.errorStream)).forEachLine {
                    if (it.isNotBlank()) logger.info(it)
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
            return listOf()
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
    fun addGain(files: List<String>, gain: Int, thread: MP3GainThread? = null): List<AddGainChange> {
        logger.info("Add $gain gain to: $files")
        try {
            if (files.size > MAX_FILES)
                throw IllegalArgumentException("Can't process more than 15 files. Given: ${files.size}")

            val cmd = mutableListOf(mp3gainPath, "-g", gain.toString())
            if (preserveTimestamp)
                cmd.add("-p")
            cmd.addAll(files)

            logger.info("call: ${cmd.toList()}")
            val currentProcess = Runtime.getRuntime().exec(cmd.toTypedArray())

            // if error stream is not used this will not work
            if (thread != null) {
                thread.setInputStream(currentProcess.errorStream)
                thread.start()
            } else {
                BufferedReader(InputStreamReader(currentProcess.errorStream)).forEachLine {
                    if (it.isNotBlank()) logger.info(it)
                }
            }

            return files.map { AddGainChange(it, gain) }
        } catch (e: Exception) {
            logger.error("Error while adding gain to: $files", e)
            return listOf()
        }
    }
}