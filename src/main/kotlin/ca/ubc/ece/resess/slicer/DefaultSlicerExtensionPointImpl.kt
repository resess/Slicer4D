package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.ui.SelectSlicingCriterionAction
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.UserDataHolder
import java.nio.file.Path

class DefaultSlicerExtensionPointImpl : SlicerExtensionPoint {
    companion object {
        @JvmStatic
        private var currentSlicer: SlicerExtensionPoint = DefaultSlicerExtensionPointImpl()

        @JvmStatic
        fun getCurrentSlicer(): SlicerExtensionPoint {
            return currentSlicer
        }

        @JvmStatic
        fun setCurrentSlicer(slicer: SlicerExtensionPoint) {
            currentSlicer = slicer
        }
    }

    private val slicer = JavaSlicer()
    override val displayName = "Slicer4J"

    override fun createSlice(project: Project,
                             settings: RunnerAndConfigurationSettings?,
                             dataContext: DataContext,
                             executor: Executor
    ): SerializedProgramSlice? {
//        // Create the execution environment
//        val configuration = // Get the desired run configuration
//        val executionEnvironment = ExecutionEnvironmentBuilder.create(executor, configuration).build()
        val builder = (if (settings == null) null else ExecutionEnvironmentBuilder.createOrNull(executor, settings))
            ?: return null
        val env = builder.activeTarget().dataContext(dataContext).build()

        val task =
            object : Task.WithResult<ProgramSlice, Exception>(project, "Executing Slicing with 'Slicer4J'", true) {
                override fun compute(indicator: ProgressIndicator): ProgramSlice? {
                    val outputDirectory = kotlin.io.path.createTempDirectory("slicer4j-outputs-")
//                    Desktop.getDesktop().open(outputDirectory.toFile())

                    // *** for temp test used only ***
//                    val testOutputDirectory = Files.createDirectories(Paths.get("src\\test\\kotlin\\ca\\ubc\\ece\\resess\\execute\\generatedFile"));
//                    getProgramSlice(indicator, testOutputDirectory);

                    return getProgramSlice(indicator, outputDirectory)
                }

                private fun getProgramSlice(indicator: ProgressIndicator, outputDirectory: Path): ProgramSlice? {
                    val slicingCriteriaLocation = (env.dataContext as UserDataHolder)
                        .getUserData(SelectSlicingCriterionAction.SLICING_CRITERIA_KEY)
                        ?: run {
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showErrorDialog(
                                    project,
                                    "Please select a slicing criteria by right-clicking on the line",
                                    "No Slicing Criteria"
                                )
                            }
                            return null
                        }

                    val staticLog = outputDirectory.resolve("slicer4j-static.log")
                    val icdgLog = outputDirectory.resolve("icdg.log")

                    indicator.text = "Instrumenting"
                    val (instrumentedState, processDirs) = slicer.instrument(env, outputDirectory, staticLog)

                    indicator.text = "Collecting trace"
                    val executionResult = instrumentedState.execute(env.executor, env.runner)!!
                    val trace = slicer.collectTrace(executionResult, outputDirectory, staticLog)

                    indicator.text = "Creating dynamic control flow graph"
                    val graph = slicer.createDynamicControlFlowGraph(icdgLog, trace, processDirs)

                    indicator.text = "Locating slicing criteria"
                    val slicingCriteria =
                        slicer.locateSlicingCriteria(graph, slicingCriteriaLocation)
                    if (slicingCriteria.isEmpty()) {
                        throw ExecutionException(
                            "Unable to locate $slicingCriteriaLocation in the dynamic control flow graph"
                        )
                    }

                    indicator.text = "Slicing"
                    return slicer.slice(project, graph, slicingCriteria, processDirs, outputDirectory)
                }
            }
        task.queue() // This runs synchronously for modal tasks
        return SerializedProgramSlice(task.result.sliceLinesUnordered,task.result.dependencies,task.result.firstLine)
    }

}