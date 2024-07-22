package ca.ubc.ece.resess.settings

import ca.ubc.ece.resess.slicer.APILayer
import ca.ubc.ece.resess.slicer.ParameterSpec
import ca.ubc.ece.resess.wrappers.Slicer4JWrapper
import ca.ubc.ece.resess.ui.EditorSliceVisualizer
import ca.ubc.ece.resess.ui.SelectParametersActionGroup
import ca.ubc.ece.resess.util.ParameterType
import ca.ubc.ece.resess.util.Statement
import ca.ubc.ece.resess.util.Variable
import ca.ubc.ece.resess.wrappers.ListOfWrapperPaths
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlin.reflect.full.createInstance


@State(
    name = "WrapperManager",
    storages = [Storage("slicerWrappers.xml")],
    reloadable = true
)

class WrapperManager : PersistentStateComponent<WrapperManager> {

    companion object {
        @JvmStatic
        private var defaultWrapper: APILayer = Slicer4JWrapper()
        private var defaultWrapperMetadata: WrapperMetadata = WrapperMetadata(
            "Slicer4J (default)",
            "ca.ubc.ece.resess.wrappers.Slicer4JWrapper",
            defaultWrapper.getConfiguration())
        private var currentWrapper: APILayer = defaultWrapper
        private var currentWrapperMetadata: WrapperMetadata = defaultWrapperMetadata

        private var extraParameters: HashMap<ParameterSpec, ArrayList<ParameterType>> = hashMapOf()

        var project: Project? = null

        var extraParametersStatus: Boolean = false
        var slicingCriterionStatus: Boolean = false

        private var slicingCriterion: Statement? = null

        //wrapper selection
        @JvmStatic
        fun getCurrentWrapper(): APILayer {
            return currentWrapper
        }

        @JvmStatic
        fun getCurrentWrapperMetadata(): WrapperMetadata {
            return currentWrapperMetadata
        }

        @JvmStatic
        fun getDefaultWrapper(): APILayer {
            return defaultWrapper
        }

        fun getDefaultWrapperMetadata(): WrapperMetadata {
            return defaultWrapperMetadata
        }

        @JvmStatic
        fun backToDefault() {
            resetParameters(defaultWrapperMetadata.specs!!.size == 0)
            currentWrapper = defaultWrapper
            currentWrapperMetadata = defaultWrapperMetadata
        }

        @JvmStatic
        fun setCurrentWrapper(wrapperInfo: WrapperMetadata) {
            resetParameters(wrapperInfo.specs!!.size == 0)
            currentWrapper = getWrapperFromPath(wrapperInfo.location!!) ?: throw IllegalArgumentException("Invalid path")
            currentWrapperMetadata = wrapperInfo
        }

        //set wrapper
        @JvmStatic
        fun setupNewWrapper(data: HashMap<String, String>) : WrapperMetadata {
            val wrapper: APILayer = getWrapperFromPath(data["location"]!!) ?: throw IllegalArgumentException("Invalid path")
            return WrapperMetadata(data["name"], data["location"], wrapper.getConfiguration())
        }

        private fun getWrapperFromPath(path: String): APILayer? {
            return try {
                val kClass = Class.forName(path).kotlin
                kClass.createInstance() as APILayer?
            } catch (e: ClassNotFoundException) {
                println("Class not found: $path")
                Messages.showMessageDialog("Wrapper not found at location $path. Make sure to add your wrapper to the project, and/or to specify its correct location",
                    "Location Error", AllIcons.General.WarningDialog)
                null
            } catch (e: InstantiationException) {
                println("Cannot instantiate class: $path. Make sure it has a no-arg constructor.")
                Messages.showMessageDialog("Cannot instantiate the wrapper at location $path. Make sure it has a no-arg constructor.",
                    "Instantiation Error", AllIcons.General.WarningDialog)
                null
            } catch (e: IllegalAccessException) {
                println("Illegal access: Cannot instantiate class: $path")
                Messages.showMessageDialog("Cannot access and instantiate wrapper at location $path. Make sure it has a no-arg constructor.",
                    "Access Error", AllIcons.General.WarningDialog)
                null
            }
        }


        @JvmStatic
        fun getAllWrapperMetadata(): List<WrapperMetadata> {
            val wrappersMetadata = mutableListOf<WrapperMetadata>()
            val paths = ListOfWrapperPaths.paths

            paths.forEach { path ->
                println(path)
                val instance = getWrapperFromPath(path)
                if (instance != null) {
                    wrappersMetadata.add(
                        WrapperMetadata(
                            instance.slicerName,
                            path,
                            instance.getConfiguration()
                        )
                    )
                }
            }
            return wrappersMetadata
        }


        //parameter specification
        fun setSlicingCriterion(statement: Statement) {
            assert(statement.slicingContext != null)
            slicingCriterionStatus = true
            slicingCriterion = statement
            assert(currentWrapper.setSlicingCriterion(statement))
            project = statement.slicingContext!!.project
            greyLining()
            getVariables()
        }


        fun setExtraParameter(pair: Pair<ParameterSpec, ArrayList<ParameterType>>) {
            extraParameters[pair.first] = pair.second

            var nbInfParams = 0
            currentWrapperMetadata.specs!!.forEach(){
                if (it.numberOfValues == 0) nbInfParams += 1
            }
            var NonInfSize = 0
            extraParameters.forEach(){
                if (it.key.numberOfValues != 0) NonInfSize += 1
            }

            if (currentWrapperMetadata.specs!!.size - nbInfParams == NonInfSize) {
                extraParametersStatus = true
                assert(currentWrapper.setParameters(extraParameters))
            }
            greyLining()
            getVariables()
        }

        private fun greyLining() {
            if (project != null && slicingCriterionStatus && extraParametersStatus) {
                println("started")
                val sliceVisualizer = EditorSliceVisualizer(this.project!!)
                sliceVisualizer.start()
            } else {
                println("false: $slicingCriterionStatus, $extraParametersStatus")
                println("metadata: ${currentWrapperMetadata.name}, ${currentWrapperMetadata.location}, ${currentWrapperMetadata.specs}")
            }
        }

        private fun resetParameters(hasExtraParameters: Boolean) {
            extraParameters = hashMapOf()
            SelectParametersActionGroup.resetChildrenMap()

            if (project != null && slicingCriterionStatus && extraParametersStatus) {
                val sliceVisualizer = EditorSliceVisualizer(project!!)
                sliceVisualizer.stop()
            }

            extraParametersStatus = !hasExtraParameters
            slicingCriterionStatus = false
        }

        @JvmStatic
        var sliceVariables : ArrayList<Variable> = ArrayList()

        @JvmStatic
        private fun getVariables() {
            sliceVariables = ArrayList()
            if (!slicingCriterionStatus || !extraParametersStatus || slicingCriterion == null || slicingCriterion!!.slicingContext == null){ return }
            val e: AnActionEvent = slicingCriterion!!.slicingContext!!

            val sliceLines = getSliceLines()
            val editor = e.getData(CommonDataKeys.EDITOR) ?: throw IllegalArgumentException("No editor found")
            val psiFile = PsiDocumentManager.getInstance(e.project!!).getPsiFile(editor.document) ?: throw IllegalArgumentException("No psi file found")

            val document = editor.document

            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    when (element) {
                        is PsiVariable -> addVariable(element)
                        is PsiReferenceExpression -> addReference(element)
                    }
                }

                private fun addVariable(variable: PsiVariable) {
                    if (variable.name != "System.out") {
                        val lineNumber = document.getLineNumber(variable.textRange.startOffset)
                        val className = PsiTreeUtil.getParentOfType(variable, PsiClass::class.java)?.name!!
                        if (sliceLines.contains(lineNumber)) {
                            sliceVariables.add(Variable(Statement(className, lineNumber), variable.name!!, true))
                        }
                    }
                }

                private fun addReference(reference: PsiReferenceExpression) {
                    if (reference.text != "System.out") {
                        val resolved = reference.resolve()
                        if (resolved is PsiVariable) {
                            val lineNumber = document.getLineNumber(reference.textRange.startOffset)
                            val className = PsiTreeUtil.getParentOfType(resolved, PsiClass::class.java)?.name!!
                            if (sliceLines.contains(lineNumber)) {
                                sliceVariables.add(Variable(Statement(className, lineNumber), reference.text, true))
                            }
                        }
                    }
                }
            })

        }

        private fun getSliceLines(): List<Int> {
            val sliceLines = ArrayList<Int>()
            var currentStatement : Statement? = currentWrapper.getFirstInSlice()?: return sliceLines

            while (currentStatement != null) {
                if (!sliceLines.contains(currentStatement.lineNo)) {
                    sliceLines.add(currentStatement.lineNo)
                }
                currentStatement = currentWrapper.nextInSlice(currentStatement)
            }

            return ArrayList(sliceLines.toSet())
        }


    }

    var slicerWrapperFields = listOf<String>("name", "location")
    var slicerWrappers = mutableListOf<WrapperMetadata>()

    init {
        slicerWrappers.add(defaultWrapperMetadata)
        slicerWrappers.addAll(getAllWrapperMetadata())
        resetParameters(currentWrapperMetadata.specs?.size != 0)
    }



    override fun getState(): WrapperManager {
        return this
    }

    override fun loadState(state: WrapperManager) {
        XmlSerializerUtil.copyBean(state, this)
    }

}

data class WrapperMetadata (
    val name: String? = null,
    val location: String? = null,
    val specs: ArrayList<ParameterSpec>? = null
) {
    fun get(key: String): Any? {
        return when (key) {
            "name" -> name
            "location" -> location
            "specs" -> specs
            else -> null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WrapperMetadata

        if (name != other.name) return false
        if (location != other.location) return false
        if (specs != other.specs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + (specs?.hashCode() ?: 0)
        return result
    }
}