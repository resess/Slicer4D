package ca.ubc.ece.resess.util

import com.intellij.util.io.exists
import junit.framework.TestCase
import java.util.zip.ZipInputStream
import kotlin.io.path.createTempDirectory

class UtilsTest : TestCase() {
    fun testUnzipAll() {
        val outputDirectory = createTempDirectory("test-unzip-all-")
        val zip = UtilsTest::class.java.getResourceAsStream("/TestExtractAll.zip")!!
        Utils.unzipAll(ZipInputStream(zip), outputDirectory)

        assertTrue(outputDirectory.resolve("1").resolve("1.txt").exists())
        assertTrue(outputDirectory.resolve("2").resolve("3").resolve("2.txt").exists())
        assertTrue(outputDirectory.resolve("3.txt").exists())
    }

    fun testReadTextReplacingLineSeparatorCrlf() {
        val outputDirectory = createTempDirectory("test-read-text-")
        val zip = UtilsTest::class.java.getResourceAsStream("/TestText.zip")!!
        Utils.unzipAll(ZipInputStream(zip), outputDirectory)
        val textFilePath = outputDirectory.resolve("TestTextCrlf.txt")
        val actual = Utils.readTextReplacingLineSeparator(textFilePath)
        val expected = "apple\nbanana"
        assertEquals(expected, actual)
    }

    fun testReadTextReplacingLineSeparatorLr() {
        val outputDirectory = createTempDirectory("test-read-text-")
        val zip = UtilsTest::class.java.getResourceAsStream("/TestText.zip")!!
        Utils.unzipAll(ZipInputStream(zip), outputDirectory)
        val textFilePath = outputDirectory.resolve("TestTextLf.txt")
        val actual = Utils.readTextReplacingLineSeparator(textFilePath)
        val expected = "apple\nbanana"
        assertEquals(expected, actual)
    }

    fun testGetFileContentSha256Crlf() {
        val outputDirectory = createTempDirectory("test-get-sha256-")
        val zip = UtilsTest::class.java.getResourceAsStream("/TestText.zip")!!
        Utils.unzipAll(ZipInputStream(zip), outputDirectory)
        val textFilePath = outputDirectory.resolve("TestTextCrlf.txt")
        val actual = Utils.getFileContentSha256(textFilePath)
        val expected = "5c573825e0a168e9e382f9b7e78e6603271634dceac792b9c92fa77a27d404f8"
        assertEquals(expected, actual)
    }

    fun testGetFileContentSha256Lf() {
        val outputDirectory = createTempDirectory("test-get-sha256-")
        val zip = UtilsTest::class.java.getResourceAsStream("/TestText.zip")!!
        Utils.unzipAll(ZipInputStream(zip), outputDirectory)
        val textFilePath = outputDirectory.resolve("TestTextLf.txt")
        val actual = Utils.getFileContentSha256(textFilePath)
        val expected = "5c573825e0a168e9e382f9b7e78e6603271634dceac792b9c92fa77a27d404f8"
        assertEquals(expected, actual)
    }
}