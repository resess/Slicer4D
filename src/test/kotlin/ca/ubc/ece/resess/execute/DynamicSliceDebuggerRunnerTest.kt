package ca.ubc.ece.resess.execute

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import junit.framework.TestCase
import javax.swing.Icon

class DynamicSliceDebuggerRunnerTest : TestCase() {
    private val runner = DynamicSliceDebuggerRunner()

    fun testCanRunTrue() {
        assertTrue(runner.canRun(DynamicSliceDebuggerExecutor.EXECUTOR_ID, DummyRunProfile()))
    }

    fun testCanRunFalse() {
        assertFalse(runner.canRun("Something", DummyRunProfile()))
    }

    private class DummyRunProfile : RunProfile {
        override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? = null

        override fun getName(): String = "DummyRunProfile"

        override fun getIcon(): Icon? = null
    }
}