package ca.ubc.ece.resess.listeners

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.Content
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebugSessionListener
import com.intellij.xdebugger.XDebuggerManagerListener
import ca.ubc.ece.resess.dbgcontroller.DppJavaDebugProcess
import ca.ubc.ece.resess.slicer.ProgramSlice
import ca.ubc.ece.resess.ui.EditorSliceVisualizer
import ca.ubc.ece.resess.ui.Icons
import ca.ubc.ece.resess.ui.dependencies.ControlDependenciesPanel
import ca.ubc.ece.resess.ui.dependencies.DataDependenciesPanel
//import ca.ubc.ece.resess.ui.dependencies.GraphPanel
import ca.ubc.ece.resess.util.Statement
import javax.swing.JComponent


class DebuggerListener : XDebuggerManagerListener {
    companion object {
        private val LOG = Logger.getInstance(DebuggerListener::class.java)
    }

    override fun processStarted(debugProcess: XDebugProcess) {
        if (debugProcess !is DppJavaDebugProcess) {
            return
        }
        val session: XDebugSession = debugProcess.session
        val project = session.project
        val sliceVisualizer = EditorSliceVisualizer(project)
        val dataDepPanel = DataDependenciesPanel(project)
        val controlDepPanel = ControlDependenciesPanel(project)
//        val graphPanel = GraphPanel()

        // Update Debugger++ tabs when paused
        session.addSessionListener(object : XDebugSessionListener {
            override fun sessionPaused() {
                ApplicationManager.getApplication().invokeAndWait {
                    updateDependenciesTabs(session, debugProcess.slice, dataDepPanel, controlDepPanel)
                }
            }
        })

        // Listen to process events to enable/disable line greying
        debugProcess.processHandler.addProcessListener(object : ProcessListener {
            override fun startNotified(processEvent: ProcessEvent) {
                initDebuggerUI(session, dataDepPanel, controlDepPanel)
                sliceVisualizer.start()
            }

            override fun processTerminated(processEvent: ProcessEvent) {}
            override fun processWillTerminate(processEvent: ProcessEvent, b: Boolean) {
                emptyDependenciesTabs(dataDepPanel, controlDepPanel)
            }

            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {}
        })
    }

    private fun initDebuggerUI(
        debugSession: XDebugSession,
        dataDepComponent: JComponent,
        controlDepComponent: JComponent
    ) {
        val ui: RunnerLayoutUi = debugSession.ui

        val sliceInfoComponent = JBTabbedPane()
        val sliceInfoTab: Content = ui.createContent(
                "sliceInfoTab",
                sliceInfoComponent,
                "Slicer4D",
                Icons.Logo,
                null
        )
//        sliceInfoComponent.addTab("Data Dep", dataDepComponent)
//        sliceInfoComponent.addTab("Control Dep", controlDepComponent)
//        sliceInfoComponent.addTab("Graph", graphComponent)
        ui.addContent(sliceInfoTab)
        sliceInfoTab.isCloseable = false
    }

    private fun updateDependenciesTabs(
        session: XDebugSession, slice: ProgramSlice,
        dataPanel: DataDependenciesPanel, controlPanel: ControlDependenciesPanel,
//        graphPanel: GraphPanel
    ) {
        // Get current position
        val currentPosition = session.currentPosition ?: return
//        val currentLineNum: Int = currentPosition.line + 1
        // Find class name
        val file = PsiManager.getInstance(session.project).findFile(currentPosition.file)
            ?: return
        val element = file.findElementAt(currentPosition.offset)
        val className = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)?.qualifiedName
        // Get dependencies
        val location = className?.let { Statement(it, currentPosition.line + 1) }
        val dependencies = slice.dependencies[location]
        val dataDependencies = dependencies?.data
        val controlDependencies = dependencies?.control
        // Update UI
        dataPanel.updateDependencies(dataDependencies, location)
        controlPanel.updateDependencies(controlDependencies, location)
//        graphPanel.updateGraph(currentLineNum, slice)
    }

    private fun emptyDependenciesTabs(
        dataPanel: DataDependenciesPanel,
        controlPanel: ControlDependenciesPanel,
//        graphPanel: GraphPanel
    ) {
        dataPanel.emptyPanel()
        controlPanel.emptyPanel()
//        graphPanel.emptyPanel()
    }
}