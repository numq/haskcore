package io.github.numq.haskcore.platform

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock

internal object Instance {
    private const val TEMP_DIR = "java.io.tmpdir"

    private const val LOCK_FILE = "haskcore.lock"

    private const val LOCK_ACCESS_MODE = "rw"

    private var lock: FileLock? = null

    private var lockFile: RandomAccessFile? = null

    val isFirst by lazy {
        try {
            val file = File(System.getProperty(TEMP_DIR), LOCK_FILE)

            lockFile = RandomAccessFile(file, LOCK_ACCESS_MODE)

            lock = lockFile?.channel?.tryLock()

            if (lock != null) {
                Runtime.getRuntime().addShutdownHook(Thread {
                    lock?.release()

                    lockFile?.close()

                    file.delete()
                })
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}