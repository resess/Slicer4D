package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice
import com.intellij.execution.Executor
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project

class TestSlicerExtensionPointImpl : SlicerExtensionPoint {
    override val displayName = "Dummy"

    override fun createSlice(project: Project,
                             settings: RunnerAndConfigurationSettings?,
                             dataContext: DataContext,
                             executor: Executor
    ): SerializedProgramSlice? {
        val ps = ProgramSlice(null, DynamicSlice(), "C:\\Users\\Pranab\\IdeaProjects\\s4d_test\\ProgramSlice (2).json" )
        return SerializedProgramSlice(ps.sliceLinesUnordered,ps.dependencies,ps.firstLine)
    }
}