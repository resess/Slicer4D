package ca.ubc.ece.resess.execute

import ca.ubc.ece.resess.settings.WrapperManager
import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.text.TextWithMnemonic
import com.intellij.openapi.wm.ToolWindowId
import ca.ubc.ece.resess.ui.Icons
import javax.swing.Icon

class DynamicSliceDebuggerExecutor : Executor() {
    companion object {
        const val EXECUTOR_ID = "DynamicSliceDebuggerExecutor"

        val instance: Executor?
            get() {
                return ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID)
            }
    }

    override fun getToolWindowId(): String = ToolWindowId.DEBUG

    override fun getToolWindowIcon(): Icon = Icons.Logo

    override fun getIcon(): Icon = Icons.Logo

    override fun getDisabledIcon(): Icon = IconLoader.getDisabledIcon(icon)

    override fun getDescription(): String = "Debug selected configuration with Slicer4J"

    override fun getActionName(): String = "Slicer4D"

    override fun getId(): String = EXECUTOR_ID

    override fun getStartActionText(): String = "Debug with Slicer4D"

    override fun getStartActionText(configurationName: String): String =
        TextWithMnemonic.parse("Debug '%s' with Slicer4D using ${WrapperManager.getCurrentWrapperMetadata().name}")
            .replaceFirst("%s", configurationName)
            .toString()

    override fun getContextActionId(): String = "DebugWithDynamicSlicing"

    override fun getHelpId(): String? = null
}