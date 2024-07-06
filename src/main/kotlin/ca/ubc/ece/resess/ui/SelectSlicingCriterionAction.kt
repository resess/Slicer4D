package ca.ubc.ece.resess.ui

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunManager
import com.intellij.execution.actions.RunConfigurationsComboBoxAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import ca.ubc.ece.resess.execute.DynamicSliceDebuggerExecutor
import ca.ubc.ece.resess.execute.RunCurrentFile
import ca.ubc.ece.resess.slicer.ProgramSlice
import ca.ubc.ece.resess.slicer.Slicer4JWrapper
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice
import ca.ubc.ece.resess.slicer.WrapperManager
import ca.ubc.ece.resess.util.Statement

class SelectSlicingCriterionAction : AnAction() {
    companion object {
        private val LOG = Logger.getInstance(SelectSlicingCriterionAction::class.java)
        val SLICING_CRITERIA_KEY = Key.create<Statement>("debuggerpp.slicing-criteria")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isVisible = SlicerActionGroup.isSlicerSelected
    }

    override fun actionPerformed(e: AnActionEvent) {
        // Selects the line the cursor is currently on, regardless of any highlighting
        val editor = e.getData(CommonDataKeys.EDITOR)!!
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)!!
        val offset = editor.caretModel.offset
        val document = editor.document
        val lineNo = document.getLineNumber(offset) + 1
        LOG.info("Selected slicing criterion line number is $lineNo")

        // TODO: show a dialog for these errors
        val element = psiFile.findElementAt(offset)
            ?: throw ExecutionException("Cannot find any element at this location")
        val clazz = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
            ?: throw ExecutionException("This location is not inside a Java class")

        val fileName = e.getData(CommonDataKeys.VIRTUAL_FILE)?.name
        println("File Name is $fileName")
        LOG.info("Class Name is ${clazz.qualifiedName}")
        startSliceDebugger(e, Statement(clazz.qualifiedName!!, lineNo, e))
    }

    private fun startSliceDebugger(e: AnActionEvent, criteria: Statement) {
        val project = e.project!!
        var selectedConfig = RunManager.getInstance(project).selectedConfiguration
        if (selectedConfig == null && RunConfigurationsComboBoxAction.hasRunCurrentFileItem(project)) {
            val psiFile = e.getData(CommonDataKeys.PSI_FILE)!!
            selectedConfig = RunCurrentFile.getRunConfigsForCurrentFile(psiFile, true).find { it != null }
        }
        if (selectedConfig == null)
            throw IllegalStateException("no selected configuration and no current file config")

        // To be retrieved later in com.intellij.openapi.progress.Task.WithResult#getProgramSlice
        (e.dataContext as UserDataHolder).putUserData(SLICING_CRITERIA_KEY, criteria)
        //TODO(change with getWrapper)
        WrapperManager.testWrapper.setSlicingCriterion(criteria)


        // Compute slice
        val slicer = WrapperManager.getCurrentWrapper()
        val serializedProgramSlice = slicer.createSlice(project, selectedConfig, e.dataContext, DynamicSliceDebuggerExecutor.instance!!)
        val programSlice = ProgramSlice(dynamicSlice = DynamicSlice(), serializedProgramSlice = serializedProgramSlice)
        ProgramSlice.setcurrentProgramSlice(programSlice)

        //If EnableSlicing, perform line greying
        if(EnableSlicingAction.isSlicingEnabled){
            val sliceVisualizer = EditorSliceVisualizer(e.project!!)
            sliceVisualizer.start()
        }

        // TO-DO make a setting such that if it is set. run the code below
//        RunnerHelper.run(
//            project,
//            selectedConfig.configuration,
//            selectedConfig,
//            e.dataContext,
//            DynamicSliceDebuggerExecutor.instance!!
//        )
    }
}