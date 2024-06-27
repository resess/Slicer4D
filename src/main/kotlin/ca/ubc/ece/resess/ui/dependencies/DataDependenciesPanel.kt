package ca.ubc.ece.resess.ui.dependencies

import ca.ubc.ece.resess.slicer.DataDependencies
import ca.ubc.ece.resess.slicer.DataDependency
import com.intellij.openapi.project.Project
import ca.ubc.ece.resess.util.SourceLocation

class DataDependenciesPanel(project: Project) : DependenciesPanel(project) {
    fun updateDependencies(dependencies: DataDependencies?, location: SourceLocation?) {
        removeAll()
        if (dependencies == null) {
            addNoDependenciesMessage("Data")
        } else {
            if (location == null) return
            addTitleLabel(location)
            updateDependencies(dependencies.from)
        }
        updateUI()
    }

    private fun updateDependencies(dependencies: Collection<DataDependency>) {
        for (dependency in dependencies) {
            if (dependency.variableName.isEmpty())
                continue
            addDependencyLine("${dependency.variableName}: ", dependency)
        }
        if (dependencies.isEmpty())
            addEmptyLabel()
    }

    fun emptyPanel() {
        emptyPanel("Data Dependencies are not available")
    }
}