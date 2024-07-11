package ca.ubc.ece.resess.listeners

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebuggerManagerListener
import ca.ubc.ece.resess.ui.EditorSliceVisualizer


class DebuggerListener : XDebuggerManagerListener {
    companion object {
        private val LOG = Logger.getInstance(DebuggerListener::class.java)
    }

    override fun processStarted(debugProcess: XDebugProcess) {
        val sliceVisualizer = EditorSliceVisualizer(debugProcess.session.project)

        // Listen to process events to enable/disable line greying
        debugProcess.processHandler.addProcessListener(object : ProcessListener {
            override fun startNotified(processEvent: ProcessEvent) {
                sliceVisualizer.start()
            }
            override fun processTerminated(processEvent: ProcessEvent) {}
            override fun processWillTerminate(processEvent: ProcessEvent, b: Boolean) {}
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {}
        })
    }
}