package ca.ubc.ece.resess.ui

import ca.ubc.ece.resess.settings.WrapperManager
import ca.ubc.ece.resess.settings.WrapperMetadata
import ca.ubc.ece.resess.slicer.ParameterSpec
import ca.ubc.ece.resess.slicer.TypeOfParameter
import ca.ubc.ece.resess.util.ParameterType
import ca.ubc.ece.resess.util.Statement
import ca.ubc.ece.resess.util.Variable
import com.intellij.execution.ExecutionException
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class WrapperManagerUI {
    companion object {
        fun getSelectSlicerAction(metadata: WrapperMetadata): AnAction {
            val name: String = metadata.name!!
            return object : AnAction(name) {
                var wasSelected: Boolean = false

                override fun actionPerformed(e: AnActionEvent) {
                    // Set the selected slicer as the active slicer
                    if (WrapperManager.getCurrentWrapperMetadata().name == name) { // if same (i.e. deselect), go back to default
                        SelectSlicerActionGroup.isCustomSlicerSelected = false
                        WrapperManager.backToDefault()
                        e.presentation.icon = AllIcons.Diff.GutterCheckBox
                    } else if (WrapperManager.getDefaultWrapperMetadata().name == name) { // if selected default, go back to default
                        SelectSlicerActionGroup.isCustomSlicerSelected = false
                        WrapperManager.backToDefault()
                        e.presentation.icon = AllIcons.Diff.GutterCheckBoxSelected
                    } else { // otherwise, selected slicer is custom
                        SelectSlicerActionGroup.isCustomSlicerSelected = true
                        WrapperManager.setCurrentWrapper(metadata)
                        e.presentation.icon = AllIcons.Diff.GutterCheckBoxSelected
                    }

                    if (!wasSelected) {
                        showInstructionsMessage(metadata.specs)
                    }
                }

                private fun showInstructionsMessage(specs: ArrayList<ParameterSpec>?) {
                    wasSelected = true
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("Follow these steps to use Slicer4D with the slicer '$name': \n\n")
                    stringBuilder.append("1. Right click on a statement or variables and:\n")
                    stringBuilder.append(" - choose 'Select Slicing Criterion' from the options\n")

                    specs?.forEach { spec ->
                        stringBuilder.append(" - choose 'Select ${spec.label}': ${spec.description}\n")
                    }

                    stringBuilder.append("2. Specify a breakpoint for debugging \n")
                    stringBuilder.append("3. Click on the 'Debug with Slicer4D' button in the toolbar")

                    val message = stringBuilder.toString()

                    Messages.showMessageDialog(
                        message,
                        "Instructions for selected slicer: '$name'",
                        AllIcons.Actions.IntentionBulb
                    )
                }
            }

        }

        fun getEditConfigurationAction(): AnAction {
            return object : AnAction("Edit Slicer Configurations") {
                override fun actionPerformed(e: AnActionEvent) {
                    e.project?.let {
                        @Suppress("warnings")
                        ShowSettingsUtil.getInstance()
                            .showSettingsDialog(it, "ca.ubc.ece.resess.settings.SlicerConfigurable")
                    }
                    this.templatePresentation.icon = AllIcons.Actions.AddList
                }
            }
        }

        fun getExtraParameterAction(spec: ParameterSpec): ParameterGetterActionInterface {
            if (spec.type == TypeOfParameter.STATEMENT)  {
                return StatementGetterAction(spec.label, spec)
            } else {
                return VariableGetterAction(spec.label, spec)
            }

        }
    }
}

interface ParameterGetterActionInterface {
    fun getStatus(): Boolean
    fun getValues(): ArrayList<ParameterType>
}

// fix? two purposes in one class (an action object + spec manager)
class StatementGetterAction(private val name: String?,
                                     private val spec: ParameterSpec) : AnAction("Select as $name"), ParameterGetterActionInterface {
    private var status: Boolean = spec.numberOfValues == 0
    private var values: ArrayList<ParameterType> = if (spec.numberOfValues == 0) ArrayList() else ArrayList(spec.numberOfValues)

    override fun getStatus(): Boolean {
        return status
    }

    override fun getValues(): ArrayList<ParameterType> {
        return values
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val statement = getStatement(e)
        if (spec.type == TypeOfParameter.STATEMENT && values.contains(statement)) {
            e.presentation.text = "Already selected as $name"
            e.presentation.isEnabled = false
            e.presentation.icon = AllIcons.Debugger.ThreadStates.Idle
        } else {
            e.presentation.text = "Select as $name"
            e.presentation.isEnabled = true
            e.presentation.icon = AllIcons.Actions.InSelection
        }
//        e.presentation.icon = if (status) AllIcons.Debugger.ThreadStates.Idle else AllIcons.Actions.InSelection
    }

    override fun actionPerformed(e: AnActionEvent) {
        if ((status && spec.numberOfValues == values.size && spec.numberOfValues != 0) || values.contains(getStatement(e))) {
            println("do nothing")
            return
        }
        //set the slicing criterion
        values.add(getStatement(e))
        if (spec.numberOfValues == values.size || spec.numberOfValues == 0) {
            status = true
            WrapperManager.setExtraParameter(Pair(spec, values))
        }
    }

    private fun getStatement(e : AnActionEvent): Statement {
        val editor = e.getData(CommonDataKeys.EDITOR)!!
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)!!
        val offset = editor.caretModel.offset
        val document = editor.document
        val lineNo = document.getLineNumber(offset) + 1

        val element = psiFile.findElementAt(offset)
        if (element == null) {
            Messages.showMessageDialog("Cannot find any element at this location",
                "Location Error", AllIcons.General.WarningDialog)
            throw ExecutionException("Cannot find any element at this location")
        }
        val clazz = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
        if (clazz == null) {
            Messages.showMessageDialog("This location is not inside a Java class",
                "Location Error", AllIcons.General.WarningDialog)
            throw ExecutionException("This location is not inside a Java class")
        }
        return Statement(clazz.qualifiedName!!, lineNo, e)
    }
}

class VariableGetterAction(private val name: String?,
                            private val spec: ParameterSpec) : ActionGroup("Select as $name", true), ParameterGetterActionInterface {
    private var status: Boolean = spec.numberOfValues == 0
    private var values: ArrayList<ParameterType> = if (spec.numberOfValues == 0) ArrayList() else ArrayList(spec.numberOfValues)

    override fun getStatus(): Boolean {
        return status
    }

    override fun getValues(): ArrayList<ParameterType> {
        return values
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        var allSelected = true
        val variables = getVariables(e)
        variables.forEach { allSelected = allSelected && (values.contains(it)) }
        if (variables.isEmpty()){
            e.presentation.text = "No variables on this line as $name"
            e.presentation.isEnabled = false
            e.presentation.icon = AllIcons.Debugger.Db_disabled_breakpoint
        } else if (allSelected) {
            e.presentation.text = "Already selected all variables as $name"
            e.presentation.isEnabled = true
            e.presentation.icon = AllIcons.Debugger.ThreadStates.Idle
        } else if (status && spec.numberOfValues != 0) {
            e.presentation.text =  "Max selections reached for $name"
            e.presentation.isEnabled = false
            e.presentation.icon = AllIcons.Debugger.ThreadStates.Idle
        } else {
            e.presentation.text = "Select as $name"
            e.presentation.isEnabled = true
            e.presentation.icon = AllIcons.Debugger.Db_muted_disabled_breakpoint
        }
//        e.presentation.icon = if (status) AllIcons.Debugger.ThreadStates.Idle else AllIcons.Actions.InSelection
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val children = mutableListOf<AnAction>()
        for (variable in getVariables(e!!)) {
            val action = object : AnAction("Select ${variable.name}") {
                override fun actionPerformed(e: AnActionEvent) {
                    if ((status && spec.numberOfValues == values.size && spec.numberOfValues != 0) || values.contains(variable)) {
                        println("do nothing")
                        return
                    }
                    values.add(variable)
                    if (spec.numberOfValues == values.size || spec.numberOfValues == 0) {
                        status = true
                        WrapperManager.setExtraParameter(Pair(spec, values))
                    }
                }
                override fun update(e: AnActionEvent) {
                    //TODO: when status, update UI to show that it cannot be selected again (e.g. grey out)
                    super.update(e)
                    if (spec.type == TypeOfParameter.VARIABLE && values.contains(variable)) {
                        e.presentation.text = "Already selected ${variable.name}"
                        e.presentation.isEnabled = false
                        e.presentation.icon = AllIcons.Actions.InSelection
                    } else {
                        e.presentation.text = "Select \"${variable.name}\""
                        e.presentation.isEnabled = true
                        e.presentation.icon = AllIcons.Actions.InSelection
                    }
//        e.presentation.icon = if (status) AllIcons.Debugger.ThreadStates.Idle else AllIcons.Actions.InSelection
                }
            }
            children.add(action)
        }
        return children.toTypedArray()
    }

    private fun getLineNo(e : AnActionEvent) : Int {
            val editor = e.getData(CommonDataKeys.EDITOR)!!
            val offset = editor.caretModel.offset
            val document = editor.document
            return document.getLineNumber(offset) + 1
    }

    private fun getVariables(e: AnActionEvent): List<Variable> {
        val variableList = ArrayList<Variable>()

        val lineNo : Int = getLineNo(e)

        val editor = e.getData(CommonDataKeys.EDITOR) ?: return variableList
        val psiFile = PsiDocumentManager.getInstance(e.project!!).getPsiFile(editor.document) ?: return variableList

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
                    val lineNumber = document.getLineNumber(variable.textRange.startOffset) + 1
                    val className = PsiTreeUtil.getParentOfType(variable, PsiClass::class.java)?.name!!
                    if (lineNumber == lineNo) {
                        variableList.add(Variable(Statement(className, lineNumber), variable.name!!, true))
                    }
                }
            }

            private fun addReference(reference: PsiReferenceExpression) {
                if (reference.text != "System.out") {
                    val resolved = reference.resolve()
                    if (resolved is PsiVariable) {
                        val lineNumber = document.getLineNumber(reference.textRange.startOffset) + 1
                        val className = PsiTreeUtil.getParentOfType(resolved, PsiClass::class.java)?.name!!
                        if (lineNumber == lineNo) {
                        variableList.add(Variable(Statement(className, lineNumber), reference.text, true))
                            }
                    }
                }
            }
        })

        return variableList
    }

}
