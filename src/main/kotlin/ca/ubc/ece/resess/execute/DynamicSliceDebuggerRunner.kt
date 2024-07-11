package ca.ubc.ece.resess.execute

import com.intellij.debugger.DebugEnvironment
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.DefaultDebugEnvironment
import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.xdebugger.*
import com.intellij.xdebugger.impl.XDebugSessionImpl
import ca.ubc.ece.resess.dbgcontroller.DppJavaDebugProcess
import ca.ubc.ece.resess.util.Patch
import java.util.concurrent.atomic.AtomicReference

class DynamicSliceDebuggerRunner : GenericDebuggerRunner() {
    companion object {
        const val ID = "DynamicSliceDebuggerRunner"
        private val LOG = Logger.getInstance(DynamicSliceDebuggerRunner::class.java)
    }

    override fun getRunnerId() = ID

    override fun canRun(executorId: String, profile: RunProfile) =
        executorId == DynamicSliceDebuggerExecutor.EXECUTOR_ID

    override fun execute(environment: ExecutionEnvironment) {
        LOG.info("Version: ${PluginManagerCore.getPlugin(PluginId.getId("ca.ubc.ece.resess"))!!.version}")
        Patch.forceSetDelegatedRunProfile(environment.runProfile, environment.runProfile)
        super.execute(environment)
    }

    override fun createContentDescriptor(
        state: RunProfileState,
        env: ExecutionEnvironment
    ): RunContentDescriptor? {
        return super.createContentDescriptor(state, env)
    }

    @Throws(ExecutionException::class)
    override fun attachVirtualMachine(
        state: RunProfileState?,
        env: ExecutionEnvironment,
        connection: RemoteConnection?,
        pollTimeout: Long
    ): RunContentDescriptor? {
        val ex = AtomicReference<ExecutionException?>()
        val result = AtomicReference<RunContentDescriptor>()
        ApplicationManager.getApplication().invokeAndWait {
            val environment: DebugEnvironment = DefaultDebugEnvironment(env, state!!, connection, pollTimeout)
            try {
                val debuggerSession =
                    DebuggerManagerEx.getInstanceEx(env.project).attachVirtualMachine(environment)
                        ?: return@invokeAndWait
                val session =
                    XDebuggerManager.getInstance(env.project).startSession(env, object : XDebugProcessStarter() {
                        override fun start(session: XDebugSession): XDebugProcess {
                            val sessionImpl = session as XDebugSessionImpl
                            val executionResult = debuggerSession.process.executionResult
                            sessionImpl.addExtraActions(*executionResult.actions)
                            if (executionResult is DefaultExecutionResult) {
                                sessionImpl.addRestartActions(*executionResult.restartActions)
                            }
                            return DppJavaDebugProcess.create(session, debuggerSession)
                        }
                    })
                result.set(session.runContentDescriptor)
            } catch (e: ExecutionException) {
                ex.set(e)
            }
        }
        if (ex.get() != null)
            throw ex.get()!!
        return result.get()
    }

}