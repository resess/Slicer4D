package ca.ubc.ece.resess.util

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible


/**
 * Unsafe operations which are use as workarounds for IntelliJ IDEA bugs/limitations
 */
class Patch {
    companion object {
        private val LOG = Logger.getInstance(Patch::class.java)
        private val DELEGATED_RUN_PROFILE_KEY: Key<RunProfile>

        init {
            val companionObject = ExecutionManagerImpl::class.companionObject
            val propertyAny = companionObject!!.memberProperties
                .find { it.name == "DELEGATED_RUN_PROFILE_KEY" }

            @Suppress("UNCHECKED_CAST")
            val property = propertyAny!! as KProperty1<Any, Key<RunProfile>>
            property.isAccessible = true
            DELEGATED_RUN_PROFILE_KEY = property.get(ExecutionManagerImpl::class.companionObjectInstance!!)
        }

        fun putNewRunner(executorId: String, runnerId: String) {
            val field = ExternalSystemUtil::class.java.getDeclaredField("RUNNER_IDS")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val runnerIds = field.get(null) as HashMap<String, String>
            runnerIds[executorId] = runnerId
            LOG.info("RUNNER_IDS after patched: $runnerIds")
        }

        // {@link RunnerAndConfigurationSettings}
        /**
         * Used as a workaround to the bug in `RunnerAndConfigurationSettings.isOfSameType`, which is
         * defined in `com/intellij/execution/impl/ExecutionManagerImpl.kt`.
         * `isOfSameType` returns `false` incorrectly when `DELEGATED_RUN_PROFILE_KEY[thisConfiguration]` is `null` and
         * `DELEGATED_RUN_PROFILE_KEY[thatConfiguration] === thisConfiguration`.
         * This function can be used as a workaround to set `DELEGATED_RUN_PROFILE_KEY[thisConfiguration]`
         * to be non-null.
         */
        fun forceSetDelegatedRunProfile(runProfile: RunProfile, runProfileToDelegate: RunProfile) {
            if (runProfile !is UserDataHolder)
                throw IllegalArgumentException("runProfile is not UserDataHolder")
            DELEGATED_RUN_PROFILE_KEY[runProfile] = runProfileToDelegate
        }
    }
}
