package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.SlicePrinter
import ca.ubc.ece.resess.util.Statement
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parents
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.io.path.pathString


class ProgramSlice(
    private val project: Project? = null,
    private val dynamicSlice: DynamicSlice,
    private val loadFromFile: String? = null,
    private val serializedProgramSlice: SerializedProgramSlice? = null
) {
    companion object {
        private val LOG = Logger.getInstance(ProgramSlice::class.java)
        private var currentProgramSlice: ProgramSlice? = null

        fun getcurrentProgramSlice(): ProgramSlice? {
            return currentProgramSlice
        }

        fun setcurrentProgramSlice(programSlice: ProgramSlice) {
            currentProgramSlice = programSlice
        }
    }

    private val sliceData: SerializedProgramSlice by lazy {
        if (loadFromFile != null) {
            loadSliceDataFromFile(loadFromFile)
        } else {
            createSliceData()
        }
    }

    val sliceLinesUnordered: Map<String, Set<Int>> by lazy { sliceData.sliceLinesUnordered }
    val dependencies: Map<Statement, Dependencies> by lazy { sliceData.dependencies }
    val firstLine: Statement? by lazy { sliceData.firstLine }
//    val dotGraphFile: File by lazy { sliceData.dotGraphFile }
//    val sliceLogFile: File by lazy { sliceData.sliceLogFile }
    private fun loadSliceDataFromFile(filePath: String): SerializedProgramSlice {
        val file = File(filePath)
        val json = file.readText()

        // Deserialize JSON data into DeserializedProgramSlice class
        val gson = GsonBuilder().create()
        val deserializedData = gson.fromJson(json, DeserializedProgramSlice::class.java)

        val sliceLinesUnordered = deserializedData.sliceLinesUnordered
        val dependencies = deserializedData.dependencies.mapKeys { entry ->
            val (clazz, lineNo) = entry.key.split(":")
            Statement(clazz, lineNo.toInt())
        }
        val firstLine = deserializedData.firstLine
//        val dotGraphFile = deserializedData.dotGraphFile
//        val sliceLogFile = deserializedData.sliceLogFile

        return SerializedProgramSlice(sliceLinesUnordered, dependencies, firstLine)
    }
    private fun createSliceData(): SerializedProgramSlice {
        if(serializedProgramSlice!=null) return serializedProgramSlice
        val sliceLinesUnordered = createSliceLinesUnordered()
        val dependenciesMap = createDependenciesMap()

        val outDir = Files.createTempDirectory("slicer4j-dotGraphFile")
        SlicePrinter.printDotGraph(outDir.pathString, dynamicSlice)
//        val dotGraphFile = File(outDir.pathString + File.separator + "slice-graph.dot")

        val logDir = Files.createTempDirectory("slicer4j-sliceLog")
        SlicePrinter.printSliceLines(logDir.pathString, dynamicSlice)
//        val sliceLogFile = File(logDir.pathString + File.separator + "slice.log")

        return SerializedProgramSlice(sliceLinesUnordered, dependenciesMap, findFirstLine())
    }

    private fun createSliceLinesUnordered(): Map<String, Set<Int>> {
        if (project == null) {
            throw IllegalStateException()
        }
        return ReadAction.compute<Map<String, Set<Int>>, Throwable> {
            val map = HashMap<String, MutableSet<Int>>()
            val documentManager = PsiDocumentManager.getInstance(project)
            val searchScope = GlobalSearchScope.allScope(project)
            val psiFacade = JavaPsiFacade.getInstance(project)
            for (sliceNode in dynamicSlice.map { x -> x.o1.o1 }) {
                if (sliceNode.javaSourceLineNo < 0)
                    continue
                val set = map.getOrPut(sliceNode.javaSourceFile) { HashSet() }
                val line = sliceNode.javaSourceLineNo - 1
                val clazz = psiFacade.findClass(sliceNode.javaSourceFile, searchScope)
                val file = clazz?.containingFile
                val document = file?.let { documentManager.getDocument(file) }
                if (document != null) {
                    val lineOffset = (document.getLineStartOffset(line) + document.getLineEndOffset(line)) / 2
                    val element = file.findElementAt(lineOffset)
                    element?.parents(false)
                        ?.forEach {
                            if (it !is PsiIfStatement) {
                                set.add(document.getLineNumber(it.startOffset))
                                set.add(document.getLineNumber(it.endOffset))
                            }
                        }
                }
                set.add(line)
            }
            return@compute map
        }
    }
    private fun createDependenciesMap(): Map<Statement, Dependencies> {
        val map = HashMap<Statement, Dependencies>()
        val entriesSeen = HashSet<Triple<String, Statement, Statement>>()
        for (entry in dynamicSlice) {
            val fromNode = entry.o1.o1
            val toNode = entry.o2.o1
            val fromLocation = Statement(fromNode.javaSourceFile, fromNode.javaSourceLineNo)
            val toLocation = Statement(toNode.javaSourceFile, toNode.javaSourceLineNo)
            val type = dynamicSlice.getEdges(entry.o1.o1.lineNo, entry.o2.o1.lineNo)
            if (!entriesSeen.add(Triple(type, fromLocation, toLocation)))
                continue

            val dependenciesFrom = map.getOrPut(fromLocation) { Dependencies() }
            val dependenciesTo = map.getOrPut(toLocation) { Dependencies() }
            when (type) {
                "data" -> run {
                    val variableName = entry.o2.o2.pathString
                    if (variableName.isNotBlank()) {
                        (dependenciesFrom.data.to as ArrayList).add(DataDependency(toLocation, variableName))
                        (dependenciesTo.data.from as ArrayList).add(DataDependency(fromLocation, variableName))
                    }
                }

                "control" -> {
                    (dependenciesFrom.control.to as ArrayList).add(ControlDependency(toLocation))
                    (dependenciesTo.control.from as ArrayList).add(ControlDependency(fromLocation))
                }

                else -> throw IllegalStateException("Unknown dependency type $type")
            }
        }
        return map
    }

    private fun findFirstLine(): Statement? {
        return dynamicSlice.order.getOrNull(0)?.o1?.let {
            Statement(it.javaSourceFile, it.javaSourceLineNo - 1)
        }
    }
    fun saveToFile(filePath: String) {
        val gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
        val json: String = gson.toJson(sliceData)

        val file = File(filePath)
        file.writeText(json)
    }
}

data class SerializedProgramSlice(
    val sliceLinesUnordered: Map<String, Set<Int>>,
    val dependencies: Map<Statement, Dependencies>,
    val firstLine: Statement?
)

data class DeserializedProgramSlice(
    val sliceLinesUnordered: Map<String, Set<Int>>,
    val dependencies: Map<String, Dependencies>,
    val firstLine: Statement?
)

class Dependencies(
    val data: DataDependencies = DataDependencies(),
    val control: ControlDependencies = ControlDependencies()
) {
    override fun equals(other: Any?) = (other is Dependencies) && data == other.data && control == other.control
    override fun hashCode() = Objects.hash(data, control)
}

class DataDependencies(
    val from: List<DataDependency> = ArrayList(),
    val to: List<DataDependency> = ArrayList()
) {
    override fun equals(other: Any?) = (other is DataDependencies) && from == other.from && to == other.to
    override fun hashCode() = Objects.hash(from, to)
}

class ControlDependencies(
    val from: List<ControlDependency> = ArrayList(),
    val to: List<ControlDependency> = ArrayList()
) {
    override fun equals(other: Any?) = (other is ControlDependencies) && from == other.from && to == other.to
    override fun hashCode() = Objects.hash(from, to)
}

abstract class Dependency(val location: Statement) {
    override fun equals(other: Any?) = (other is Dependency) && location == other.location
    override fun hashCode() = location.hashCode()
}

class ControlDependency(location: Statement) : Dependency(location)

class DataDependency(location: Statement, val variableName: String) : Dependency(location) {
    override fun equals(other: Any?) = (other is DataDependency)
            && location == other.location && variableName == other.variableName

    override fun hashCode() = Objects.hash(location, variableName)
}