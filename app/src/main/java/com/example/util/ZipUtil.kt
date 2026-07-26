package com.example.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtil {

    /**
     * Zips all files in sourceDir into outputZipFile.
     */
    fun zipDirectory(sourceDir: File, outputZipFile: File) {
        if (!sourceDir.exists()) return
        outputZipFile.parentFile?.mkdirs()

        FileOutputStream(outputZipFile).use { fos ->
            BufferedOutputStream(fos).use { bos ->
                ZipOutputStream(bos).use { zos ->
                    zipFileOrFolder(sourceDir, sourceDir.path, zos)
                }
            }
        }
    }

    private fun zipFileOrFolder(fileOrFolder: File, basePath: String, zos: ZipOutputStream) {
        if (fileOrFolder.isDirectory) {
            val children = fileOrFolder.listFiles() ?: return
            for (child in children) {
                zipFileOrFolder(child, basePath, zos)
            }
        } else {
            val relativePath = fileOrFolder.path.substring(basePath.length + 1).replace("\\", "/")
            val zipEntry = ZipEntry(relativePath)
            zos.putNextEntry(zipEntry)
            FileInputStream(fileOrFolder).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    /**
     * Unzips an InputStream into targetDir.
     * Returns a map of relative path -> text content (if readable as text).
     */
    fun unzipToDirectory(inputStream: InputStream, targetDir: File): Map<String, String> {
        if (!targetDir.exists()) targetDir.mkdirs()

        val resultMap = mutableMapOf<String, String>()

        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(2048)

            while (entry != null) {
                val entryName = entry.name.replace("\\", "/").trimStart('/')
                // Skip directory entries or hidden system files (__MACOSX, .DS_Store)
                if (!entry.isDirectory && !entryName.startsWith("__MACOSX") && !entryName.contains(".DS_Store")) {
                    val outputFile = File(targetDir, entryName)
                    outputFile.parentFile?.mkdirs()

                    FileOutputStream(outputFile).use { fos ->
                        var count: Int
                        while (zis.read(buffer).also { count = it } != -1) {
                            fos.write(buffer, 0, count)
                        }
                    }

                    // Try reading as string if it's a code/text file
                    try {
                        val content = outputFile.readText()
                        resultMap[entryName] = content
                    } catch (_: Exception) {
                        // Binary file like image, skipped from DB text content
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        return resultMap
    }
}
