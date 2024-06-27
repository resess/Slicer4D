package ca.ubc.ece.resess.ui.dependencies

import ca.ubc.ece.resess.slicer.ControlDependencies
import ca.ubc.ece.resess.slicer.ControlDependency
import com.intellij.openapi.project.Project
import ca.ubc.ece.resess.util.SourceLocation


class ControlDependenciesPanel(project: Project) : DependenciesPanel(project) {
    fun updateDependencies(dependencies: ControlDependencies?, location: SourceLocation?) {
        removeAll()
        if (dependencies == null) {
            addNoDependenciesMessage("Control")
        } else {
            if (location == null) return
            addTitleLabel(location)
            updateDependencies(dependencies.from)
        }
        updateUI()
    }

    private fun updateDependencies(dependencies: Collection<ControlDependency>) {
        for (dependency in dependencies) {
            addDependencyLine("", dependency)
        }
        if (dependencies.isEmpty())
            addEmptyLabel()
    }

    fun emptyPanel() {
        emptyPanel("Control Dependencies are not available")
    }
}