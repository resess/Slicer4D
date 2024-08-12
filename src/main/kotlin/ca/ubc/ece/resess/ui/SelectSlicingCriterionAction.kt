package ca.ubc.ece.resess.ui

import ca.ubc.ece.resess.listeners.DebuggerListener.Companion.isDebugging
import com.intellij.execution.ExecutionException
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import ca.ubc.ece.resess.settings.WrapperManager
import ca.ubc.ece.resess.settings.WrapperManager.Companion.project
import ca.ubc.ece.resess.util.Statement
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages

class SelectSlicingCriterionAction : AnAction() {
    companion object {
        private val LOG = Logger.getInstance(SelectSlicingCriterionAction::class.java)
        var slicingCriterion: Statement? = null
        var slicingCriterionStatus: Boolean = false
        fun resetSlicingCriterion() {
            slicingCriterion = null
            slicingCriterionStatus = false
            if (EditorSliceVisualizer.isRunning) {
                val sliceVisualizer = EditorSliceVisualizer(project!!)
                sliceVisualizer.stop()
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        val statement = Statement.getStatement(e)
        if (slicingCriterion == statement) {
            assert(slicingCriterionStatus)
            e.presentation.text = "Deselect this line as Slicing Criterion"
            e.presentation.isEnabled = true
        } else {
            e.presentation.text = "Select Slicing Criterion"
            e.presentation.isEnabled = true
        }

        if (isDebugging) {
            e.presentation.isEnabled = false
        }
//        e.presentation.icon = if (status) AllIcons.Debugger.ThreadStates.Idle else AllIcons.Actions.InSelection
    }

    override fun actionPerformed(e: AnActionEvent) {
        //set the slicing criterion
        val statement = Statement.getStatement(e)

        if (slicingCriterion == statement) { //slicing criterion already selected and same line
            resetSlicingCriterion()
            return
        }
        slicingCriterion = statement
        slicingCriterionStatus = true
        WrapperManager.setSlicingCriterion(statement)
    }
}