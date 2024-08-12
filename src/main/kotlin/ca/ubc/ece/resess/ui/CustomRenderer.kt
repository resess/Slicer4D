package ca.ubc.ece.resess.ui

import ca.ubc.ece.resess.listeners.DebuggerListener.Companion.isDebugging
import ca.ubc.ece.resess.settings.WrapperManager
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.ui.tree.ValueDescriptor
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener
import com.intellij.debugger.ui.tree.render.NodeRendererImpl
import com.intellij.openapi.project.Project
import com.sun.jdi.Type

class CustomRenderer : NodeRendererImpl("CustomRenderer", true) {
    override fun calcLabel(valueDescriptor: ValueDescriptor, evaluationContext: EvaluationContext, descriptorLabelListener: DescriptorLabelListener): String {
        WrapperManager.sliceVariables.forEach() {
            if (it.name == valueDescriptor.name) { // fix: add more robust comparison (with className, or type)
                return valueDescriptor.value.toString()
            }
        }
        return "[not in slice]"
    }

    @Override
    override fun isApplicable(type: Type): Boolean {
        val project: Project = WrapperManager.project?: return false
        val debuggerSession = DebuggerManagerEx.getInstanceEx(project).sessions.firstOrNull()?.xDebugSession
        return debuggerSession?.debugProcess?.javaClass?.simpleName == "DppJavaDebugProcess"
    }

    @Override
    override fun isEnabled(): Boolean {
        return isDebugging
    }

    override fun getUniqueId(): String {
        return "ca.ubc.ece.resess.ui.NameAdjuster"
    }

}