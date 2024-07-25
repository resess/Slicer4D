package ca.ubc.ece.resess.util

import com.intellij.execution.ExecutionException
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

class Statement @JvmOverloads constructor(
    val clazz: String,
    val lineNo: Int, // 0 based
    val slicingContext: AnActionEvent? = null): ParameterType {
    companion object {
        fun getStatement(e: AnActionEvent): Statement {
            val editor = e.getData(CommonDataKeys.EDITOR)!!
            val psiFile = e.getData(CommonDataKeys.PSI_FILE)!!
            val offset = editor.caretModel.offset
            val document = editor.document
            val lineNo = document.getLineNumber(offset) + 1

            val element = psiFile.findElementAt(offset)
            if (element == null) {
                ApplicationManager.getApplication().invokeLater() {
                    Messages.showMessageDialog(
                        "Cannot find any element at this location",
                        "Location Error", AllIcons.General.WarningDialog
                    )
                }
                throw ExecutionException("Cannot find any element at this location")
            }
            val clazz = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
            if (clazz == null) {
                ApplicationManager.getApplication().invokeLater() {
                    Messages.showMessageDialog(
                        "This location is not inside a Java class",
                        "Location Error", AllIcons.General.WarningDialog
                    )
                }
                throw ExecutionException("This location is not inside a Java class")
            }
            return Statement(clazz.qualifiedName!!, lineNo, e)
        }
    }


    override fun toString() = "$clazz:$lineNo"

    override fun equals(other: Any?) = (other is Statement)
            && clazz == other.clazz
            && lineNo == other.lineNo

    override fun hashCode() = Objects.hash(clazz, lineNo)
}
