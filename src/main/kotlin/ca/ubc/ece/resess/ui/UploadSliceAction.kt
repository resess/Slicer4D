package ca.ubc.ece.resess.ui

import ca.ubc.ece.resess.slicer.ProgramSlice
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class UploadSliceAction : AnAction("Upload Slice") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
        val fileChooser = FileChooserFactory.getInstance().createFileChooser(fileChooserDescriptor, project, null)
        val selectedFile: VirtualFile? = fileChooser.choose(project).firstOrNull()

        selectedFile?.let {
            val filePath = it.path
            processSliceFile(filePath, project)
        }
    }

    private fun processSliceFile(filePath: String, project: Project) {
        try {
            ProgramSlice.setcurrentProgramSlice(ProgramSlice(null, DynamicSlice(), filePath))
            //If EnableSlicing, perform line greying
            if(EnableSlicingAction.isSlicingEnabled){
                val programSlice=ProgramSlice.getcurrentProgramSlice()
                val sliceVisualizer = programSlice?.let { EditorSliceVisualizer(project, it) }
                sliceVisualizer?.start()
            }
        } catch (e: Exception) {
            val errorMessage = "Please follow JSON template: ${e.message}"
            showErrorMessage(errorMessage, project)
        }
    }
    private fun showErrorMessage(message: String, project: Project) {
        val notification = Notification("SlicePlugin", "Error uploading Slice", message, NotificationType.ERROR)
        Notifications.Bus.notify(notification, project)
    }
}
