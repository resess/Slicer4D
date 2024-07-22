package ca.ubc.ece.resess.ui

import com.intellij.execution.ExecutionException
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import ca.ubc.ece.resess.settings.WrapperManager
import ca.ubc.ece.resess.util.Statement
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.Messages

class SelectSlicingCriterionAction : AnAction() {
    companion object {
        private val LOG = Logger.getInstance(SelectSlicingCriterionAction::class.java)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        // Selects the line the cursor is currently on, regardless of any highlighting
        val editor = e.getData(CommonDataKeys.EDITOR)!!
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)!!
        val offset = editor.caretModel.offset
        val document = editor.document
        val lineNo = document.getLineNumber(offset) + 1
        LOG.info("Selected slicing criterion line number is $lineNo")

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

        val fileName = e.getData(CommonDataKeys.VIRTUAL_FILE)?.name
        println("File Name is $fileName")
        LOG.info("Class Name is ${clazz.qualifiedName}")

        //set the slicing criterion
        WrapperManager.setSlicingCriterion(Statement(clazz.qualifiedName!!, lineNo, e))
    }
}