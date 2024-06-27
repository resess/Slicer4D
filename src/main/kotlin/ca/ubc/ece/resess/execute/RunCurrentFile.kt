package ca.ubc.ece.resess.execute

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.ContainerUtil

// Copied from com.intellij.execution.ExecutorRegistryImpl
class RunCurrentFile {
    companion object {
        private val CURRENT_FILE_RUN_CONFIGS_KEY: Key<RunCurrentFileInfo> =
            Key.create("RunCurrentFile.CURRENT_FILE_RUN_CONFIGS_KEY")

        private class RunCurrentFileInfo constructor(
            val psiModCount: Long,
            val runConfigs: List<RunnerAndConfigurationSettings>
        )

        fun getRunConfigsForCurrentFile(
            psiFile: PsiFile,
            resetCache: Boolean
        ): List<RunnerAndConfigurationSettings?> {
            if (resetCache) {
                psiFile.putUserData(CURRENT_FILE_RUN_CONFIGS_KEY, null)
            }

            // Without this cache, an expensive method `ConfigurationContext.getConfigurationsFromContext()` is called too often for 2 reasons:
            // - there are several buttons on the toolbar (Run, Debug, Profile, etc.), each runs ExecutorAction.update() during each action update session
            // - the state of the buttons on the toolbar is updated several times a second, even if no files are being edited

            // The following few lines do pretty much the same as CachedValuesManager.getCachedValue(), but it's implemented without calling that
            // method because it appeared to be too hard to satisfy both IdempotenceChecker.checkEquivalence() and CachedValueStabilityChecker.checkProvidersEquivalent().
            // The reason is that RunnerAndConfigurationSettings class doesn't implement equals(), and that CachedValueProvider would need to capture
            // ConfigurationContext, which doesn't implement equals() either.
            // Effectively, we need only one boolean value: whether the action is enabled or not, so it shouldn't be a problem that
            // RunnerAndConfigurationSettings and ConfigurationContext don't implement equals() and this code doesn't pass CachedValuesManager checks.
            val psiModCount = PsiModificationTracker.getInstance(psiFile.project).modificationCount
            var cache = psiFile.getUserData(CURRENT_FILE_RUN_CONFIGS_KEY)
            if (cache == null || cache.psiModCount != psiModCount) {
                // The 'Run current file' feature doesn't depend on the caret position in the file, that's why ConfigurationContext is created like this.
                val configurationContext = ConfigurationContext(psiFile)

                // The 'Run current file' feature doesn't reuse existing run configurations (by design).
                val configurationsFromContext = configurationContext.createConfigurationsFromContext()
                val runConfigs = if (configurationsFromContext != null) ContainerUtil.map(
                    configurationsFromContext
                ) { obj: ConfigurationFromContext -> obj.configurationSettings } else emptyList()
                val vFile = psiFile.virtualFile
                val filePath = vFile?.path
                for (config in runConfigs) {
                    (config as RunnerAndConfigurationSettingsImpl).filePathIfRunningCurrentFile = filePath
                }
                cache = RunCurrentFileInfo(psiModCount, runConfigs)
                psiFile.putUserData(CURRENT_FILE_RUN_CONFIGS_KEY, cache)
            }
            return cache.runConfigs
        }
    }
}