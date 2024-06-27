package ca.ubc.ece.resess.starters

import ca.ubc.ece.resess.listeners.BreakpointListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class ProjectOpener : StartupActivity {
    override fun runActivity(project: Project) {
        BreakpointListener.setProject(project)
    }
}
