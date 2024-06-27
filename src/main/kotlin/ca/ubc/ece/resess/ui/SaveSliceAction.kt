package ca.ubc.ece.resess.ui
import ca.ubc.ece.resess.slicer.ProgramSlice
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class SaveSliceAction : AnAction("Save Slice") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val projectDir = project.basePath ?: return
        val fileName = "ProgramSlice"
        val outputFile = getUniqueFileName(projectDir, fileName)
        println(outputFile)
        try {
            ProgramSlice.getcurrentProgramSlice()?.saveToFile(outputFile)
            val errorMessage = "File name- $outputFile"
            showSuccessMessage(errorMessage, project)
        }
        catch (_: Exception) {
        }
    }
    private fun getUniqueFileName(directory: String, baseName: String): String {
        val jsonExtension = ".json"
        val file = File(directory, "$baseName$jsonExtension")
        if (!file.exists()) {
            return file.absolutePath
        }

        val nameSuffix = AtomicInteger(1)
        var uniqueName: String
        do {
            uniqueName = "$baseName (${nameSuffix.getAndIncrement()})$jsonExtension"
        } while (File(directory, uniqueName).exists())

        return File(directory, uniqueName).absolutePath
    }

    private fun showSuccessMessage(message: String, project: Project) {
        val notification = Notification("SlicePlugin","Slice saved successfully", message, NotificationType.INFORMATION)
        Notifications.Bus.notify(notification, project)
    }

}
