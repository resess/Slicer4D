package ca.ubc.ece.resess.ui

import com.google.protobuf.InvalidProtocolBufferException
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import ca.ubc.ece.resess.settings.PluginSettingsStateComponent
import ca.ubc.ece.resess.settings.SlicerConfigurable

class SliceMenu: DefaultActionGroup() {
    private val settings = service<PluginSettingsStateComponent>()

    init {
        addChildren()
    }

    private fun addChildren(){

        for ((index, sliceProvider) in settings.sliceProviders.withIndex()){
            super.addAction(object: AnAction(sliceProvider["name"]){
                override fun actionPerformed(e: AnActionEvent) {
//                    println("You selected $sliceProvider!!!")
//                    state.startSliceProvider(index, e.project!!, false)
//                    val listOfDataRequired = state.getRequiredDataTypes()
//                    collectSlicingData(listOfDataRequired, e.project!!)
                }
            })
        }

        super.addSeparator();

        super.addAction(object : AnAction("Edit Slicer Configurations"){
            override fun actionPerformed(e: AnActionEvent) {
                val slicerConfigurable = SlicerConfigurable()
                // Add your logic for "Edit Configurations" here
                e.project?.let {
                    ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), "ca.ubc.ece.resess.settings.SlicerConfigurable");
                }
            }
        })


    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        // TODO: Change this to be more efficient
        this.removeAll()
        addChildren()
    }
}