package ca.ubc.ece.resess.listeners

import ca.ubc.ece.resess.ui.EditorSliceVisualizer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDocumentManager
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointListener
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XBreakpointManager


object BreakpointListener : XBreakpointListener<XBreakpoint<*>> {

    private var currentProject: Project? = null

    override fun breakpointAdded(breakpoint: XBreakpoint<*>) {
        if(currentProject==null){
            return
        }
        val debuggerManager: XDebuggerManager = XDebuggerManager.getInstance(currentProject!!)
        val breakpointManager: XBreakpointManager = debuggerManager.breakpointManager
        if (isBreakpointOnGreyedOutLine(breakpoint)) {
            ApplicationManager.getApplication().invokeLater {
                val result = Messages.showOkCancelDialog(
                    currentProject,
                    "Adding a breakpoint to a greyed-out line is not recommended. Do you want to proceed?",
                    "Invalid Breakpoint Addition",
                    "Add Anyway",
                    "Don't Add",
                    null
                )
                if (result == Messages.CANCEL) {
                    ApplicationManager.getApplication().runWriteAction {
                        breakpointManager.removeBreakpoint(breakpoint)
                    }
                }
            }
        }
    }

    private fun isBreakpointOnGreyedOutLine(breakpoint: XBreakpoint<*>): Boolean {
        val editor = FileEditorManager.getInstance(currentProject!!).selectedTextEditor ?: return false
        val lineNumber = breakpoint.sourcePosition?.line ?: return false

        // Get the current file from the editor's document
        val psiFile = PsiDocumentManager.getInstance(currentProject!!).getPsiFile(editor.document)
        val currentFilePath = psiFile?.virtualFile?.path

        // Check if the current file path matches the breakpoint file path
        if (currentFilePath != breakpoint.sourcePosition?.file?.path) {
            return false
        }

        for (highlighter in editor.markupModel.allHighlighters) {
            if (highlighter.targetArea == HighlighterTargetArea.LINES_IN_RANGE) {
                if (EditorSliceVisualizer.GREY_OUT_COLOR == highlighter.getTextAttributes(null)?.foregroundColor) {
                    if(editor.document.getLineNumber(highlighter.startOffset)==lineNumber)
                        return true
                    if(editor.document.getLineNumber(highlighter.endOffset)==lineNumber)
                        return true
                }
            }
        }
        return false
    }
    // Add a setter method for setting the project variable
    fun setProject(project: Project) {
        currentProject = project
    }
}
