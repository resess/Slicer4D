package ca.ubc.ece.resess.slicer

import com.intellij.execution.Executor
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project

interface SlicerExtensionPoint {
    val displayName: String
    fun createSlice(project: Project, settings: RunnerAndConfigurationSettings?, dataContext: DataContext, executor: Executor): SerializedProgramSlice?
}
