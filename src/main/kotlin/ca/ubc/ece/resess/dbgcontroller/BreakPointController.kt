package ca.ubc.ece.resess.dbgcontroller

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import ca.ubc.ece.resess.util.SourceLocation
import ca.ubc.ece.resess.util.Utils

class BreakPointController(private val debugProcess: DebugProcessImpl) {
    companion object {
        private val LOG = Logger.getInstance(BreakPointController::class.java)
    }

    fun addBreakpoint(location: SourceLocation) {
        val project = debugProcess.project
        val psiFile = DumbService.getInstance(project).computeWithAlternativeResolveEnabled<PsiFile, Exception> {
            Utils.findPsiFile(location.clazz, project)
        }
        if (psiFile == null) {
            LOG.error("Cannot find the PSI file for ${location.clazz}")
            return
        }
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
        val breakpointManager = DebuggerManagerEx.getInstanceEx(project).breakpointManager
        val b: LineBreakpoint<*>? = breakpointManager.addLineBreakpoint(document, location.lineNo)
        if (b == null) {
            LOG.info("Unable to add breakpoint for $location")
            return
        }
    }
}