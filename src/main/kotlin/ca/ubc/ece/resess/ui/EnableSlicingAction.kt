package ca.ubc.ece.resess.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.Project


class EnableSlicingAction : ToggleAction("Enable Slicing") {
    companion object {
        var isSlicingEnabled= true
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        // Return the current state of slicing
        return isSlicingEnabled
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        // Update the slicing enabled/disabled state
        isSlicingEnabled = state

        if (isSlicingEnabled) {
            // Enable slicing
            e.project?.let { enableSlicing(it) }
        } else {
            // Disable slicing
            e.project?.let { disableSlicing(it) }
        }
    }

    private fun enableSlicing(project: Project) {
        // If ProgramSlice is not null, enable line greying
        EditorSliceVisualizer(project).start()
    }

    private fun disableSlicing(project: Project) {
        // Disable line greying
        EditorSliceVisualizer(project).stop()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val presentation = e.presentation
        //TODO: Fix visibility depending on the selected slicer
//        presentation.isVisible = SlicerActionGroup.isCustomSlicerSelected
        val icon = if (SlicerActionGroup.isCustomSlicerSelected) AllIcons.Diff.GutterCheckBoxSelected else AllIcons.Diff.GutterCheckBox
        presentation.icon = icon
    }


}


