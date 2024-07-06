package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.slicer.dynamic.core.graph.Parser
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice
import ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer
import com.intellij.util.io.exists
import com.intellij.util.io.readText
import junit.framework.TestCase
import ca.ubc.ece.resess.util.Statement
import ca.ubc.ece.resess.util.Utils
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.jar.JarInputStream
import java.util.zip.ZipInputStream
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

abstract class SlicerTest : TestCase() {
    companion object {
        private val loggerPath: String
        private val modelsPath: String
        private val stubDroidPath: String
        private val taintWrapperPath: String

        init {
            val loggerFile = kotlin.io.path.createTempFile("slicer4-logger-", ".jar")
            val loggerJar = Slicer::class.java.getResourceAsStream("/DynamicSlicingLogger.jar")!!
            Files.copy(loggerJar, loggerFile, StandardCopyOption.REPLACE_EXISTING)
            loggerPath = loggerFile.toString()

            val modelsDirectory = createTempDirectory("slicer4-models-")
            val modelsZip = Slicer::class.java.getResourceAsStream("/models.zip")!!
            Utils.unzipAll(ZipInputStream(modelsZip), modelsDirectory)
            modelsPath = modelsDirectory.toString()
            stubDroidPath = modelsDirectory.resolve("summariesManual").toString()
            taintWrapperPath = modelsDirectory.resolve("EasyTaintWrapperSource.txt").toString()
        }
    }

    private val slicer = JavaSlicer()

    private val outputDirectory = createTempDirectory("slicer4j-outputs-")
    private val staticLog = outputDirectory.resolve("slicer4j-static.log")
    private val outJarPath = outputDirectory.resolve("instrumented.jar")
    private val stdoutLog = outputDirectory.resolve("instrumented-stdout.log")
    private val icdgLog = outputDirectory.resolve("icdg.log")

    protected fun runTest(
        jarPathName: String, slicingFromLine: Statement,
        expectedRawTrace: String?, expectedSliceLog: String?, expectedDependenciesLogSha256: String?
    ): DynamicSlice {
        val jarPath = this.javaClass.classLoader.getResource(jarPathName)!!.path
        val processDirs = Collections.singletonList(jarPath)

        // Open the folder for debugging
        // Desktop.getDesktop().open(outputDirectory.toFile())

        // Check instrumentation
        assertFalse(outJarPath.exists())
        slicer.instrumentJar(jarPath, staticLog.pathString, outputDirectory, outJarPath.pathString)
        assertTrue(outJarPath.exists())

        val jar = outJarPath.pathString.replace("\\", "/")
        val mainClass = JarInputStream(BufferedInputStream(FileInputStream(jarPath)))
            .manifest.mainAttributes.getValue("Main-Class")

        // Run and get log
        val process = Runtime.getRuntime().exec("java -cp $jar $mainClass")
        stdoutLog.bufferedWriter().use { output ->
            InputStreamReader(process.inputStream).use { input ->
                input.copyTo(output)
            }
        }
        process.waitFor()

        // save trace
        val trace = Parser.readFile(stdoutLog.pathString, staticLog.pathString)
        assertNotNull(trace)

        val traceLog = outputDirectory.resolve("trace.log")
        assertFalse(traceLog.exists())
        slicer.saveTrace(trace, outputDirectory)
        assertTrue(traceLog.exists())

        val rawTraceLog = outputDirectory.resolve("raw-trace.log")
        assertFalse(rawTraceLog.exists())
        slicer.extractRawTrace(stdoutLog, outputDirectory)
        if (expectedRawTrace != null) {
            assertEquals(expectedRawTrace, rawTraceLog.readText())
        }

        // get ICDG Graph
        val icdg = slicer.createDynamicControlFlowGraph(icdgLog, trace, Collections.singletonList(jarPath))
        assertNotNull(icdg)

        // Get slicing location
        val slicingCriteria = slicer.locateSlicingCriteria(icdg, slicingFromLine)
        assertNotNull(slicingCriteria)

        // Slice!
        val dynamicSlice = slicer.slice(
            outputDirectory.pathString, icdg, processDirs,
            slicingCriteria.map { s -> s.lineNo }, stubDroidPath, taintWrapperPath,
            null, null, true, false
        )
        assertNotNull(dynamicSlice)

        // Check slicer output
        val actualSliceLog = Utils.readTextReplacingLineSeparator(outputDirectory.resolve("slice.log"))
        if (expectedSliceLog != null) {
            assertEquals(HashSet(expectedSliceLog.split("\n")), HashSet(actualSliceLog.split("\n")))
        }
        if (expectedDependenciesLogSha256 != null) {
            assertEquals(
                expectedDependenciesLogSha256,
                Utils.getFileContentSha256(outputDirectory.resolve("slice-dependencies.log"))
            )
        }

        return dynamicSlice
    }
}