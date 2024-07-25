package ca.ubc.ece.resess.ui

import ca.ubc.ece.resess.settings.WrapperManager
import ca.ubc.ece.resess.settings.WrapperMetadata
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class SelectSlicerActionGroup : ActionGroup() {

    companion object {
        var isCustomSlicerSelected = false
    }

    private val childrenMap: HashMap<WrapperMetadata, AnAction> = hashMapOf()

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
//        val settings = service<WrapperManager>()
        val slicerWrappers = WrapperManager.slicerWrappers

        val children = mutableListOf<AnAction>()

        // Add separator and heading
        children.add(Separator("Available Slicers"))
        for (metadata in slicerWrappers) {
            val name = metadata.name
            val currMetadata = WrapperManager.getCurrentWrapperMetadata()

            if (childrenMap.keys.contains(metadata)) { // If the slicer alr has AnAction, no need to create it again
                val action = childrenMap[metadata]!!
                action.templatePresentation.icon = if (currMetadata.name == name) AllIcons.Diff.GutterCheckBoxSelected else AllIcons.Diff.GutterCheckBox
                children.add(action)
                continue
            }


            val action = WrapperManagerUI.getSelectSlicerAction(metadata)
            action.templatePresentation.icon = if (currMetadata.name == name) AllIcons.Diff.GutterCheckBoxSelected else AllIcons.Diff.GutterCheckBox
            childrenMap[metadata] = action
            children.add(action)
        }

        // Add "Add Slicer" action
        children.add(Separator())
        children.add(WrapperManagerUI.getEditConfigurationAction())
        return children.toTypedArray()
    }
}










