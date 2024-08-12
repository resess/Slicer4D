package ca.ubc.ece.resess.ui

import ca.ubc.ece.resess.listeners.DebuggerListener.Companion.isDebugging
import ca.ubc.ece.resess.settings.WrapperManager
import ca.ubc.ece.resess.settings.WrapperMetadata
import ca.ubc.ece.resess.slicer.ParameterSpec
import ca.ubc.ece.resess.slicer.TypeOfParameter
import ca.ubc.ece.resess.util.ParameterType
import ca.ubc.ece.resess.util.Statement
import ca.ubc.ece.resess.util.Variable
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.Messages

//

class WrapperManagerUI {
    companion object {
        fun getSelectSlicerAction(metadata: WrapperMetadata): AnAction {
            val name: String = metadata.name!!
            return object : AnAction(name) {
                var wasSelected: Boolean = false

                override fun actionPerformed(e: AnActionEvent) {
                    // Set the selected slicer as the active slicer
                    if (WrapperManager.getCurrentWrapperMetadata() == metadata) { // if same (i.e. deselect), go back to default
                        println("deselected wrapper ${metadata.name}")
                        SelectSlicerActionGroup.isCustomSlicerSelected = false
                        WrapperManager.backToDefault()
                        e.presentation.icon = AllIcons.Diff.GutterCheckBox
                    } else if (WrapperManager.getDefaultWrapperMetadata() == metadata) { // if selected default, go back to default
                        println("went back to default wrapper: ${metadata.name}")
                        SelectSlicerActionGroup.isCustomSlicerSelected = false
                        WrapperManager.backToDefault()
                        e.presentation.icon = AllIcons.Diff.GutterCheckBoxSelected
                    } else { // otherwise, selected slicer is custom
                        println("selected wrapper ${metadata.name}")
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
                    stringBuilder.append("1. Right click on a statement or variables and choose 'Select Slicing Criterion' from the options\n")
                    if (!specs.isNullOrEmpty()) { // not null nor empty
                        stringBuilder.append("\n2. This slicer needs ${specs.size} parameters. Right click on a statement or variables and: \n")
                        specs.forEach { spec ->
                            stringBuilder.append(" - choose 'Select ${spec.label}': ${spec.description}\n") // [${if (spec.type == TypeOfParameter.STATEMENT) "Statement" else "Variable"}]
                        }
                        stringBuilder.append("\n")
                    }
                    stringBuilder.append("${if (!specs.isNullOrEmpty()) "3." else "2."} Specify a breakpoint for debugging \n")
                    stringBuilder.append("${if (!specs.isNullOrEmpty()) "4." else "3."} Click on the 'Debug with Slicer4D' button in the toolbar")

                    val message = stringBuilder.toString()
                    ApplicationManager.getApplication().invokeLater() {
                        Messages.showMessageDialog(
                            message,
                            "Instructions for selected slicer: '$name'",
                            AllIcons.Actions.IntentionBulb
                        )
                    }
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
            //selection of extra parameters while debugging allowed -- handled by slicer wrapper
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
    fun setStatus(status: Boolean)
    fun getValues(): ArrayList<ParameterType>
    fun setValues(values: ArrayList<ParameterType>)
    fun removeValue(value: ParameterType)
}

// fix? two purposes in one class (an action object + spec manager)
class StatementGetterAction(private val name: String?,
                                     private val spec: ParameterSpec) : AnAction("Select as $name"), ParameterGetterActionInterface {
    private var status: Boolean = spec.isOptional()
    private var values: ArrayList<ParameterType> = if (spec.isOptional()) ArrayList() else ArrayList(spec.numberOfValues)

    override fun getStatus(): Boolean {
        return status
    }

    override fun setStatus(status: Boolean) {
        this.status = status
    }

    override fun getValues(): ArrayList<ParameterType> {
        return values
    }

    override fun setValues(values: ArrayList<ParameterType>) {
        this.values = values
    }

    override fun removeValue(value: ParameterType) {
        if (spec.numberOfValues != 0) { // if the number of values is capped
            if (isDebugging) { // if debugging, do not allow removal of capped parameters
                ApplicationManager.getApplication().invokeLater {
                    Messages.showMessageDialog("Cannot remove capped parameters while debugging", "Removal Error", Messages.getErrorIcon())
                }
                return
            } // otherwise, remove the value
            assert(values.remove(value)) { "Value could not be removed: not found" }
            assert(values.size < spec.numberOfValues)
            status = false
            WrapperManager.removedCappedExtraParameter(spec)
        } else { // if the number of values is unlimited, remove the value and send it to the wrapper
            values.remove(value)
            WrapperManager.setExtraParameter(Pair(spec, values))
        }
    }


    override fun update(e: AnActionEvent) {
        super.update(e)
        val statement = Statement.getStatement(e)
        if (values.contains(statement)) {
            e.presentation.text = "Remove from $name"
            e.presentation.isEnabled = true
            e.presentation.icon = AllIcons.Diff.Remove
        } else if (status && !spec.isOptional()) {
            e.presentation.text =  "Max selections reached for $name"
            e.presentation.isEnabled = false
            e.presentation.icon = AllIcons.Debugger.ThreadStates.Idle
        } else {
            e.presentation.text = "Select as $name"
            e.presentation.isEnabled = true
            e.presentation.icon = AllIcons.Actions.InSelection
        }
        if (isDebugging && spec.numberOfValues != 0) {
            e.presentation.isEnabled = false
        }
//        e.presentation.icon = if (status) AllIcons.Debugger.ThreadStates.Idle else AllIcons.Actions.InSelection
    }

    override fun actionPerformed(e: AnActionEvent) {
        val statement : Statement = Statement.getStatement(e)
        if (values.contains(statement)) {
            removeValue(statement)
            return
        }
        if ((status && spec.numberOfValues == values.size && spec.numberOfValues != 0) || values.contains(statement)) {
            println("do nothing")
            return
        }

        values.add(statement)
        if (spec.numberOfValues == values.size || spec.isOptional()) {
            status = true
            WrapperManager.setExtraParameter(Pair(spec, values))
        }
    }
}

class VariableGetterAction(private val name: String?,
                            private val spec: ParameterSpec) : ActionGroup("Select as $name", true), ParameterGetterActionInterface {
    private var status: Boolean = spec.isOptional()
    private var values: ArrayList<ParameterType> = if (spec.isOptional()) ArrayList() else ArrayList(spec.numberOfValues)

    override fun getStatus(): Boolean {
        return status
    }

    override fun setStatus(status: Boolean) {
        this.status = status
    }

    override fun getValues(): ArrayList<ParameterType> {
        return values
    }

    override fun setValues(values: ArrayList<ParameterType>) {
        this.values = values
    }

    override fun removeValue(value: ParameterType) {
        if (spec.numberOfValues != 0) { // if the number of values is capped
            println("is debugging:$isDebugging")
            if (isDebugging) { // if debugging, do not allow removal of capped parameters
                ApplicationManager.getApplication().invokeLater {
                    Messages.showMessageDialog("Cannot remove capped parameters while debugging", "Removal Error", Messages.getErrorIcon())
                }
                return
            } // otherwise, remove the value
            assert(values.remove(value)) { "Value could not be removed: not found" }
            assert(values.size < spec.numberOfValues)
            status = false
            WrapperManager.removedCappedExtraParameter(spec)
        } else { // if the number of values is unlimited, remove the value and send it to the wrapper
            values.remove(value)
            WrapperManager.setExtraParameter(Pair(spec, values))
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        val variables = Variable.getVariablesInSingleLine(e)
        val atLeastOneSelected = variables.intersect(values).isNotEmpty()

        if (variables.isEmpty()){
            e.presentation.text = "No variables on this line ($name)"
            e.presentation.isEnabled = false
            e.presentation.icon = AllIcons.Actions.InSelection
        } else if (status && spec.numberOfValues != 0) {
            e.presentation.text =  "Max selections reached for $name"
            if (atLeastOneSelected) {
                e.presentation.isEnabled = true
            } else {
                e.presentation.isEnabled = false
            }
            e.presentation.icon = AllIcons.Debugger.ThreadStates.Idle
        } else {
            e.presentation.text = "Select as $name"
            e.presentation.isEnabled = true
            e.presentation.icon = AllIcons.Actions.InSelection
        }

        if (isDebugging && spec.numberOfValues != 0) {
            e.presentation.isEnabled = false
        }
//        e.presentation.icon = if (status) AllIcons.Debugger.ThreadStates.Idle else AllIcons.Actions.InSelection
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val children = mutableListOf<AnAction>()
        for (variable in Variable.getVariablesInSingleLine(e!!)) {
            val action = object : AnAction("Select ${variable.name}") {
                override fun actionPerformed(e: AnActionEvent) {
                    if (values.contains(variable)) {
                        removeValue(variable)
                        return
                    }
                    if ((status && spec.numberOfValues == values.size && spec.numberOfValues != 0) || values.contains(variable)) {
                        println("do nothing")
                        return
                    }
                    values.add(variable)
                    if (spec.numberOfValues == values.size || spec.isOptional()) {
                        status = true
                        WrapperManager.setExtraParameter(Pair(spec, values))
                    }
                }
                override fun update(e: AnActionEvent) {
                    super.update(e)
                    if (values.contains(variable)) {
                        e.presentation.text = "Remove ${variable.name}"
                        e.presentation.isEnabled = true
                        e.presentation.icon = AllIcons.Diff.Remove
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

}
