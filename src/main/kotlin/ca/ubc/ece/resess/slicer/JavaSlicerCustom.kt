package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.ui.SelectSlicingCriterionAction
import ca.ubc.ece.resess.util.SourceLocation
import com.intellij.execution.Executor
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import es.upv.mist.slicing.cli.Slicer
import org.apache.commons.cli.ParseException


class JavaSlicerCustom : SlicerExtensionPoint {
    // Set the name which will be displayed in Available Slicers list
    override val displayName = "JavaSlicer"

    // Modify the below function to implement custom slicer
    override fun createSlice(project: Project,
                             settings: RunnerAndConfigurationSettings?,
                             dataContext: DataContext,
                             executor: Executor
    ): SerializedProgramSlice? {

        val slicingCriteriaLocation = getSlicingCriteria(settings, dataContext, executor)
        // Complete the respective functions below using the slicingCriteriaLocation
        val classPath = getClassPath(dataContext);
        println(classPath)
        // Map set of lineNumbers in slice to the respective file paths
        val sliceLinesUnordered = createSliceLinesUnordered(classPath, slicingCriteriaLocation)

        // Map the dependencies to respective sourceLocations
        val dependencies = createDependenciesMap(slicingCriteriaLocation)

        // sourceLocation of the first line in slice
        val firstLine = getFirstLine(slicingCriteriaLocation)

        return SerializedProgramSlice(sliceLinesUnordered,dependencies,firstLine)
    }

    private fun createSliceLinesUnordered(classPath: String, slicingCriteriaLocation: SourceLocation?): Map<String, Set<Int>> {
        val map = HashMap<String, MutableSet<Int>>()
        try {
            val arg = classPath+"#"+slicingCriteriaLocation!!.lineNo.toString()
            println(arg)

            val slicer = Slicer("-c", arg)// Provide the desired slicing criterion
            val javaMap: HashMap<String, Set<Int>> = slicer.slice()
            javaMap.forEach { (key, value) ->
                val modifiedKey = key.removeSuffix(".java")
                map[modifiedKey] = value.toMutableSet()
            }
        } catch (e: ParseException) {
            // Handle any parsing errors
            e.printStackTrace()
        }
        /*
        Add set of lineNumbers in the map with file path as key
        NOTE: Only in this map the line numbers should be 0 indexed, Line 1 in IDE means line 0 in the map, and so on
        */
        return map
    }

    private fun createDependenciesMap(slicingCriteriaLocation: SourceLocation?): Map<SourceLocation, Dependencies> {
        val map = HashMap<SourceLocation, Dependencies>()
        /*
        Add Dependencies in the map.
        Refer end of template for definition of Dependencies class
        */
        return map
    }

    private fun getFirstLine(slicingCriteriaLocation: SourceLocation?): SourceLocation? {
        val clazz = ""
        val lineNumber = 0
        /*
        Set the above class and line number appropriately
        Return the sourceLocation of the first line in slice.
        Refer end of template for definition of SourceLocation class
        */
        return SourceLocation(clazz,lineNumber)
    }
}

/*
data class SerializedProgramSlice(
    val sliceLinesUnordered: Map<String, Set<Int>>,
    val dependencies: Map<SourceLocation, Dependencies>,
    val firstLine: SourceLocation?,
)

class SourceLocation(val clazz: String, val lineNo: Int) {
    override fun toString() = "$clazz:$lineNo"

    override fun equals(other: Any?) = (other is SourceLocation)
            && clazz == other.clazz
            && lineNo == other.lineNo

    override fun hashCode() = Objects.hash(clazz, lineNo)
}

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

abstract class Dependency(val location: SourceLocation) {
    override fun equals(other: Any?) = (other is Dependency) && location == other.location
    override fun hashCode() = location.hashCode()
}

class ControlDependency(location: SourceLocation) : Dependency(location)

class DataDependency(location: SourceLocation, val variableName: String) : Dependency(location) {
    override fun equals(other: Any?) = (other is DataDependency)
            && location == other.location && variableName == other.variableName

    override fun hashCode() = Objects.hash(location, variableName)
}
*/

private fun getSlicingCriteria(settings: RunnerAndConfigurationSettings?,
                               dataContext: DataContext,
                               executor: Executor): SourceLocation? {
    val builder = (if (settings == null) null else ExecutionEnvironmentBuilder.createOrNull(executor, settings))
        ?: return null
    val env = builder.activeTarget().dataContext(dataContext).build()
    return (env.dataContext as UserDataHolder).getUserData(SelectSlicingCriterionAction.SLICING_CRITERIA_KEY)
}

private fun getClassPath(dataContext: DataContext): String {
    val psiFile = dataContext.getData(CommonDataKeys.PSI_FILE)
    val virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
    val editor = dataContext.getData(CommonDataKeys.EDITOR)

    if (psiFile != null && virtualFile != null && editor != null) {
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset)
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
        if (containingClass != null) {
            val qualifiedName = containingClass.qualifiedName
            if (qualifiedName != null) {
                val classFile = containingClass.containingFile
                val classPath = classFile.virtualFile.path
                return classPath
            }
        }
    }

    // Return an empty string if the classpath is not found
    return ""
}