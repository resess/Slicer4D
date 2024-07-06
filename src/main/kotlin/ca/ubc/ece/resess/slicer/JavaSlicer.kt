package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.slicer.dynamic.core.accesspath.AccessPath
import ca.ubc.ece.resess.slicer.dynamic.core.framework.FrameworkModel
import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph
import ca.ubc.ece.resess.slicer.dynamic.core.graph.Parser
import ca.ubc.ece.resess.slicer.dynamic.core.graph.Trace
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.SlicePrinter
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.SlicingWorkingSet
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance
import ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer
import ca.ubc.ece.resess.slicer.dynamic.slicer4j.instrumenter.JavaInstrumenter
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.jar.JarApplicationCommandLineState
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.io.write
import org.jetbrains.java.decompiler.IdeaDecompiler
import soot.Type
import ca.ubc.ece.resess.util.Statement
import ca.ubc.ece.resess.util.Utils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.jar.JarInputStream
import java.util.zip.InflaterOutputStream
import java.util.zip.ZipInputStream
import kotlin.io.path.*

class JavaSlicer {
    companion object {
        private val LOG = Logger.getInstance(JavaSlicer::class.java)
    }

    private val loggerPath: String
    private val modelsPath: String
    private val stubDroidPath: String
    private val taintWrapperPath: String

    init {
        val loggerFile = createTempFile("slicer4-logger-", ".jar")
        val loggerJar = JavaSlicer::class.java.getResourceAsStream("/DynamicSlicingLogger.jar")!!
        Files.copy(loggerJar, loggerFile, StandardCopyOption.REPLACE_EXISTING)
        loggerPath = loggerFile.toString()

        val modelsDirectory = createTempDirectory("slicer4-models-")
        val modelsZip = JavaSlicer::class.java.getResourceAsStream("/models.zip")!!
        Utils.unzipAll(ZipInputStream(modelsZip), modelsDirectory)
        modelsPath = modelsDirectory.toString()
        stubDroidPath = modelsDirectory.resolve("summariesManual").toString()
        taintWrapperPath = modelsDirectory.resolve("EasyTaintWrapperSource.txt").toString()
    }

    /*
     * TODO: Find a way for IntelliJ to consider this a build task that does not need to be repeated if we've already
     * instrumented this JAR before.
     */
    fun instrument(
        env: ExecutionEnvironment,
        outputDirectory: Path,
        staticLog: Path
    ): Pair<RunProfileState, List<String>> {
        val state = env.state!!
        val processDirs: List<String>
        val sootOutputDirectory = outputDirectory.resolve("soot-output")
        val instrumentationOptions = ""
        sootOutputDirectory.toFile().mkdir()
        when (state) {
            is JarApplicationCommandLineState -> {
                val params = state.javaParameters
                val outJarPath = outputDirectory.resolve("instrumented.jar").pathString
                params.mainClass = JarInputStream(BufferedInputStream(FileInputStream(params.jarPath)))
                    .manifest.mainAttributes.getValue("Main-Class")
                JavaInstrumenter(outJarPath)
                    .instrumentJar(
                        instrumentationOptions,
                        staticLog.pathString,
                        params.jarPath,
                        loggerPath,
                        sootOutputDirectory.pathString
                    )
                params.classPath.add(outJarPath)
                processDirs = Collections.singletonList(params.jarPath)
            }

            is JavaCommandLineState -> {
                val params = state.javaParameters
                val jdkPath = params.jdkPath
                val instrumentClassPaths = params.classPath.pathList.filterNot {
                    it.startsWith(jdkPath) ||
                            it.matches(Regex(".*[\\\\/]junit-[\\d.]*\\.jar")) || // Skip JUnit
                            it.matches(Regex(".*[\\\\/]hamcrest-core-[\\d.]*\\.jar")) || // Skip hamcrest
                            it.matches(Regex(".*[\\\\/]com.jetbrains.intellij.idea[\\\\/]ideaIC[\\\\/].*\\.jar")) // Skip Idea
                }
                LOG.info("Instrument Class paths: $instrumentClassPaths")
                val instrumentedClasPaths = JavaInstrumenter()
                    .instrumentClassPaths(
                        instrumentationOptions,
                        staticLog.pathString,
                        instrumentClassPaths,
                        loggerPath,
                        sootOutputDirectory.pathString
                    )
                instrumentClassPaths.forEach { params.classPath.remove(it) }
                params.classPath.addAll(instrumentedClasPaths)
                processDirs = instrumentClassPaths
            }

            else -> throw ExecutionException("Unable to instrument this type of RunProfileState")
        }
//        decompileAll(env.project, sootOutputDirectory) // Optional, for debugging purposes
        return Pair(state, processDirs)
    }

    fun collectTrace(executionResult: ExecutionResult, outputDirectory: Path, staticLog: Path): Trace {
        val stdoutLog = outputDirectory.resolve("instrumented-stdout.log")
        val stderrLog = outputDirectory.resolve("instrumented-stderr.log")

        stdoutLog.bufferedWriter().use { stdWriter ->
            stderrLog.bufferedWriter().use { errWriter ->
                executionResult.processHandler.addProcessListener(object : ProcessAdapter() {
                    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                        if (outputType === ProcessOutputTypes.STDOUT) {
                            stdWriter.write(event.text)
                        } else if (outputType == ProcessOutputTypes.STDERR) {
                            errWriter.write(event.text)
                        }
                    }
                })
                executionResult.processHandler.startNotify()
                executionResult.processHandler.waitFor()
            }
        }

        val trace = Parser.readFile(stdoutLog.pathString, staticLog.pathString)
        saveTrace(trace, outputDirectory)
        extractRawTrace(stdoutLog, outputDirectory)

        return trace
    }

    fun instrumentJar(inJarPath: String, staticLogPath: String, outputDirectory: Path, outJarPath: String) {
        val sootOutputDirectory = outputDirectory.resolve("soot-output")
        sootOutputDirectory.toFile().mkdir()
        JavaInstrumenter(outJarPath)
            .instrumentJar(
                "",
                staticLogPath,
                inJarPath,
                loggerPath,
                sootOutputDirectory.pathString
            )
    }

    fun createDynamicControlFlowGraph(output: Path, trace: Trace, processDirs: List<String>): DynamicControlFlowGraph {
        Slicer.prepare(processDirs)
        val graph = DynamicControlFlowGraph()
        graph.createDCFG(trace)
        LOG.debug("size of the trace after loading: " + graph.mapNumberUnits.keys.size)
        Slicer.printGraph(graph, output.pathString)
        return graph
    }

    fun locateSlicingCriteria(graph: DynamicControlFlowGraph, criteria: Statement): List<StatementInstance> {
        return graph.mapNumberUnits.values
            .filter { statement -> statement.javaSourceFile == criteria.clazz && statement.javaSourceLineNo == criteria.lineNo }
            .toList()
    }

    fun slice(
        project: Project,
        icdg: DynamicControlFlowGraph,
        slicingCriteria: List<StatementInstance>,
        processDirs: List<String>,
        outDir: Path
    ): ProgramSlice {
        val slice = slice(
            outDir.pathString, icdg, processDirs,
            slicingCriteria.map { s -> s.lineNo }, stubDroidPath, taintWrapperPath,
            null, null, true, false
        )
        return ProgramSlice(project, slice)
    }

    private fun decompileAll(project: Project, sootOutput: Path) {
        val decompiler = IdeaDecompiler()
        Files.walk(sootOutput).forEach { f ->
            if (f.name.endsWith(".class")) {
                val clazzVf = VfsUtil.findFile(f, true)!!
                ApplicationManager.getApplication().invokeAndWait {
                    IdeaDecompiler.LegalBurden().beforeFileOpened(FileEditorManager.getInstance(project), clazzVf)
                }
                f.resolveSibling(f.name.dropLast(".class".length) + "_decompiled.java")
                    .write(decompiler.getText(clazzVf))
            }
        }
    }

    fun saveTrace(trace: Trace, outputDirectory: Path) {
        outputDirectory.resolve("trace.log")
            .bufferedWriter().use { writer ->
                for (statement in trace) {
                    writer.write(statement.toString())
                    writer.write("\n")
                }
            }
    }

    fun extractRawTrace(stdoutLog: Path, outputDirectory: Path) {
        val rawTraceLog = outputDirectory.resolve("raw-trace.log")
        rawTraceLog.outputStream().use { os ->
            val inflaterOutputStream = InflaterOutputStream(os)
            stdoutLog.bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (line.contains(" ZLIB: ")) {
                        val encoded = line.split(" ZLIB: ")[1]
                        val decoded = Base64.getDecoder().decode(encoded)
                        inflaterOutputStream.write(decoded)
                    }
                }
            }
        }
    }

    fun slice(
        outDir: String, icdg: DynamicControlFlowGraph, processDirectories: List<String?>?,
        backwardSlicePositions: List<Int?>, stubDroidPath: String?, taintWrapperPath: String?,
        frameworkPath: String?, variableString: String?, frameworkModel: Boolean, sliceOnce: Boolean
    ): DynamicSlice {
        Slicer.prepare(processDirectories)
        val slicer = Slicer()
        slicer.setVariableString(variableString ?: "*")

        FrameworkModel.setStubDroidPath(stubDroidPath)
        FrameworkModel.setTaintWrapperFile(taintWrapperPath)
        FrameworkModel.setExtraPath(frameworkPath)

        /* Process slicing criteria */
        val stmts: MutableList<StatementInstance> = ArrayList()
        for (backSlicePos: Int? in backwardSlicePositions) {
            stmts.add(icdg.mapNoUnits(backSlicePos!!))
        }

        val variables: MutableList<String> = ArrayList()
        if ("*" != variableString && variableString != null) {
            val split = variableString.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (s: String in split) {
                variables.add("$$s")
            }
        }

        val accessPaths: MutableSet<AccessPath> = HashSet()
        for (v: String? in variables) {
            for (stmt: StatementInstance in stmts) {
                accessPaths.add(AccessPath(v, object : Type() {
                    override fun toString(): String {
                        return "SlicingCriterionType"
                    }
                }, stmt.lineNo, AccessPath.NOT_DEFINED, stmt))
            }
        }

        /* Start slicing */
        val dynamicSlice = slicer.slice(
            icdg,
            frameworkModel,
            false,
            false,
            sliceOnce,
            stmts,
            accessPaths,
            SlicingWorkingSet(false)
        )

        /* Save results to files */
        SlicePrinter.printSlices(dynamicSlice)
        SlicePrinter.printSliceGraph(dynamicSlice)
        SlicePrinter.printDotGraph(outDir, dynamicSlice)
        SlicePrinter.printSliceLines(outDir, dynamicSlice)
        SlicePrinter.printRawSlice(outDir, dynamicSlice)
        SlicePrinter.printSliceWithDependencies(outDir, dynamicSlice, backwardSlicePositions)
        SlicePrinter.printToCSV(
            outDir + File.separator + "result_s_" +
                    DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss").format(LocalDateTime.now()) + ".csv",
            dynamicSlice
        )

        return dynamicSlice
    }
}