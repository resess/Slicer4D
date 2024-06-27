package ca.ubc.ece.resess.ui

import ca.ubc.ece.resess.slicer.DefaultSlicerExtensionPointImpl
import ca.ubc.ece.resess.slicer.SlicerExtensionPoint
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class SlicerActionGroup : ActionGroup() {

    companion object {
        var isSlicerSelected = false
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val slicerExtensions = ExtensionPointName<SlicerExtensionPoint>("ca.ubc.ece.resess.slicerExtensionPoint").extensions
        var currentSlicer = DefaultSlicerExtensionPointImpl.getCurrentSlicer()

        val children = mutableListOf<AnAction>()

        // Add separator and heading
        children.add(Separator("Available Slicers"))
//        super.addSeparator()

        for (slicer in slicerExtensions) {
//            val action = object : AnAction(slicer.displayName) {
            val action = object : AnAction("Slicer4J") {
                override fun actionPerformed(e: AnActionEvent) {
                    // Set the selected slicer as the active slicer
                    isSlicerSelected = !isSlicerSelected
                    DefaultSlicerExtensionPointImpl.setCurrentSlicer(slicer)
                    currentSlicer = slicer
                    e.presentation.icon = AllIcons.Diff.GutterCheckBoxSelected
                    val message =
                        "Follow these steps to use Slicer4D with the slicer 'Slicer4J': \n\n" +
                                "1. Right click on a statement and choose 'Select Slicing Criterion' from the options \n" +
                                "2. Specify a breakpoint for debugging \n" +
                                "3. Click on the 'Debug with Slicer4D' button in the toolbar"

                    Messages.showMessageDialog(message, "Instructions for selected slicer: 'Slicer4J'", AllIcons.Actions.IntentionBulb)
                }
            }
//            action.templatePresentation.icon = if (slicer.displayName == currentSlicer.displayName) AllIcons.Diff.GutterCheckBoxSelected else AllIcons.Diff.GutterCheckBox
            action.templatePresentation.icon = if (isSlicerSelected) AllIcons.Diff.GutterCheckBoxSelected else AllIcons.Diff.GutterCheckBox
//            action.templatePresentation.icon = AllIcons.Diff.GutterCheckBox
            children.add(action)
        }

        // Add "Add Slicer" action
        children.add(AddSlicerAction())
e
        return children.toTypedArray()
    }
}

class AddSlicerAction : AnAction("Add Custom Slicer", "Steps to add custom slicer", AllIcons.General.Add){
    override fun actionPerformed(e: AnActionEvent) {
        val message =
                "1. Clone this repository - https://github.com/resess/2023_debugger_study/tree/revamp\n\n" +
                "2. Open slicer4D as a new project in IntelliJ IDE\n\n" +
                "3. Follow the instructions in README to add custom slicer\n\n" +
                "4. Select 'Run Plugin' run configuration, run by pressing the green run button in the toolbar\n\n" +
                "5. An IDE instance will open in a new window with your custom slicer present in the 'Available Slicers' list"

        Messages.showMessageDialog(message, "Add Slicer Instructions", Messages.getInformationIcon())
    }
}









