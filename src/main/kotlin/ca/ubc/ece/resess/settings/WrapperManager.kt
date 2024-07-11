package ca.ubc.ece.resess.settings

import ca.ubc.ece.resess.slicer.APILayer
import ca.ubc.ece.resess.slicer.ParameterSpec
import ca.ubc.ece.resess.slicer.Slicer4JWrapper
import ca.ubc.ece.resess.ui.SlicerActionGroup.Companion.isCustomSlicerSelected
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.*
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.Messages
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlin.reflect.full.createInstance

@State(
    name = "WrapperManager",
    storages = [Storage("slicerWrappers.xml")],
    reloadable = true
)

class WrapperManager : PersistentStateComponent<WrapperManager> {

    companion object {
        @JvmStatic
        private var defaultWrapper: APILayer = Slicer4JWrapper()
        private var defaultWrapperMetadata: WrapperMetadata = WrapperMetadata(
            "Slicer4J (default)",
            "ca.ubc.ece.resess.slicer.Slicer4JWrapper",
            defaultWrapper.getConfiguration())

        private var currentWrapper: APILayer = defaultWrapper
        private var currentWrapperMetadata: WrapperMetadata = defaultWrapperMetadata

        @JvmStatic
        fun getCurrentWrapper(): APILayer {
            return currentWrapper
        }

        @JvmStatic
        fun getCurrentWrapperMetata(): WrapperMetadata {
            return currentWrapperMetadata
        }

        @JvmStatic
        fun getDefaultWrapper(): APILayer {
            return defaultWrapper
        }

        fun getDefaultWrapperMetadata(): WrapperMetadata {
            return defaultWrapperMetadata
        }

        @JvmStatic
        fun backToDefault() {
            currentWrapper = defaultWrapper
            currentWrapperMetadata = defaultWrapperMetadata
        }

        @JvmStatic
        fun setCurrentWrapper(wrapperInfo: Pair<APILayer, WrapperMetadata>) {
            currentWrapper = wrapperInfo.first
            currentWrapperMetadata = wrapperInfo.second
        }

        @JvmStatic
        fun setupNewWrapper(data: HashMap<String, String>) : Pair<APILayer, WrapperMetadata> {
            val wrapper: APILayer = getWrapperFromPath(data["location"]!!) ?: throw IllegalArgumentException("Invalid path")
            return Pair(wrapper, WrapperMetadata(data["name"], data["location"], wrapper.getConfiguration()))
        }

        private fun getWrapperFromPath(path: String): APILayer? {
            return try {
                val kClass = Class.forName(path).kotlin
                kClass.createInstance() as APILayer?
            } catch (e: ClassNotFoundException) {
                println("Class not found: $path")
                Messages.showMessageDialog("Wrapper not found at location $path. Make sure to add your wrapper to the project, and/or to specify its correct location",
                    "Location Error", AllIcons.General.WarningDialog)
                null
            } catch (e: InstantiationException) {
                println("Cannot instantiate class: $path. Make sure it has a no-arg constructor.")
                Messages.showMessageDialog("Cannot instantiate the wrapper at location $path. Make sure it has a no-arg constructor.",
                    "Instantiation Error", AllIcons.General.WarningDialog)
                null
            } catch (e: IllegalAccessException) {
                println("Illegal access: Cannot instantiate class: $path")
                Messages.showMessageDialog("Cannot access and instantiate wrapper at location $path. Make sure it has a no-arg constructor.",
                    "Access Error", AllIcons.General.WarningDialog)
                null
            }
        }

    }
    
    var slicerWrapperFields = listOf<String>("name", "location")
    var slicerWrappers = mutableListOf<Pair<APILayer, WrapperMetadata>>()

    init {
        slicerWrappers.add(Pair(defaultWrapper, defaultWrapperMetadata))
    }
    //TODO: test with list of actual parameter specs

//    .add(ParameterSpec(
//    "test variable",
//    TypeOfParameter.VARIABLE,
//    "description",
//    "default",
//    "required",
//    "choices"
//    ))


    override fun getState(): WrapperManager {
        return this
    }

    override fun loadState(state: WrapperManager) {
        XmlSerializerUtil.copyBean(state, this)
    }

}

data class WrapperMetadata (
    val name: String? = null,
    val location: String? = null,
    val specs: ArrayList<ParameterSpec>? = null,
) {
    fun get(key: String): Any? {
        return when (key) {
            "name" -> name
            "location" -> location
            "specs" -> specs
            else -> null
        }
    }
}

class WrapperManagerUI {
    companion object {
        fun getSelectSlicerAction(instance: APILayer, metadata: WrapperMetadata): AnAction {
            val name: String = metadata.name!!
            return object : AnAction(name) {
                var wasSelected: Boolean =
                    false //TODO: fix by adding a var/list/map with AnAction, and checks if action already created prev -> create or take.

                override fun actionPerformed(e: AnActionEvent) {
                    // Set the selected slicer as the active slicer
                    if (WrapperManager.getCurrentWrapperMetata().name == name) { // if same (i.e. deselect), go back to default
                        isCustomSlicerSelected = false
                        WrapperManager.backToDefault()
                        e.presentation.icon = AllIcons.Diff.GutterCheckBox
                    } else if (WrapperManager.getDefaultWrapperMetadata().name == name) { // if selected default, go back to default
                        isCustomSlicerSelected = false
                        WrapperManager.backToDefault()
                        e.presentation.icon = AllIcons.Diff.GutterCheckBoxSelected
                    } else { // otherwise, selected slicer is custom
                        isCustomSlicerSelected = true
                        WrapperManager.setCurrentWrapper(Pair(instance, metadata))
                        e.presentation.icon = AllIcons.Diff.GutterCheckBoxSelected
                    }

                    if (!wasSelected) {
                        showInstructionsMessage()
                    }
                }

                private fun showInstructionsMessage() {
                    //TODO: add instructions for custom slicer using ParameterSpec
                    wasSelected = true
                    val message =
                        "Follow these steps to use Slicer4D with the slicer '$name': \n\n" +
                                "1. Right click on a statement and choose 'Select Slicing Criterion' from the options \n" +
                                "2. Specify a breakpoint for debugging \n" +
                                "3. Click on the 'Debug with Slicer4D' button in the toolbar"

                    Messages.showMessageDialog(
                        message,
                        "Instructions for selected slicer: '$name'",
                        AllIcons.Actions.IntentionBulb
                    )
                }
            }

        }
        fun getEditConfigurationAction(): AnAction {
            return object : AnAction("Edit Slicer Configurations"){
                override fun actionPerformed(e: AnActionEvent) {
                    e.project?.let {
                        ShowSettingsUtil.getInstance().showSettingsDialog(it, "ca.ubc.ece.resess.settings.SlicerConfigurable");
                    }
                    this.templatePresentation.icon = AllIcons.Actions.AddList
                }
            }
        }
    }
}
