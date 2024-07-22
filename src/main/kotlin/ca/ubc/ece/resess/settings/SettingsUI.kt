package ca.ubc.ece.resess.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.openapi.ui.Messages
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.table.AbstractTableModel

class SettingsUI: JPanel() {
    var modified = false
    val settings = service<WrapperManager>()
    var uiSliceProviders = settings.slicerWrappers.toMutableList()
    val table = JBTable(object: AbstractTableModel() {
        override fun getRowCount(): Int {
            return uiSliceProviders.size
        }

        override fun getColumnCount(): Int {
            return settings.slicerWrapperFields.size
        }

        override fun getColumnName(column: Int): String {
            return settings.slicerWrapperFields[column]
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return true
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val key = settings.slicerWrapperFields[columnIndex]
            val sliceProviderInfo = uiSliceProviders[rowIndex]
            val result = sliceProviderInfo.get(key) ?: return ""
            return result
        }
    })
    private val toolbarDecorator = ToolbarDecorator.createDecorator(table)

    fun createComponent(): JComponent {
        toolbarDecorator.setAddAction {
            val dialogWrapper = AddSliceProviderDialog(settings.slicerWrapperFields)
            val result = dialogWrapper.showAndGet()
            if(!result){
                return@setAddAction
            }
            
            val data: WrapperMetadata = WrapperManager.setupNewWrapper(dialogWrapper.data) // add specs using getConfig()

            uiSliceProviders.add(data) // add wrapper and metadata to uiSliceProviders (but not main list, as "apply" is not called yet)
            table.updateUI()
            modified = true

        }
        toolbarDecorator.setRemoveAction(object: AnActionButtonRunnable{
            override fun run(t: AnActionButton?) {
                val selectedRow = table.selectedRow
                if(selectedRow <= 0){
                    if (selectedRow == 0) {
                        Messages.showMessageDialog(
                            "Cannot remove the default slicer",
                            "Removal Error",
                            AllIcons.General.WarningDialog
                        )
                    }
                    return
                }
                uiSliceProviders.removeAt(selectedRow)
                table.updateUI()
                modified = true
            }
        })

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
            add(toolbarDecorator.createPanel())
        }
    }

    fun isModified(): Boolean {
        return modified
    }

    fun apply() {
        modified = false
        settings.slicerWrappers = uiSliceProviders.toMutableList()
        table.updateUI()
    }

    fun reset() {
        // Implement logic to reset the settings
        modified = false
        uiSliceProviders = settings.slicerWrappers.toMutableList()
        table.updateUI()
    }

    fun dispose() {
        // Implement logic to dispose resources
    }
}

class AddSliceProviderDialog(var fields: List<String>) : DialogWrapper(true) {
    var data = HashMap<String, String>()

    init {
        title = "Add Slice Provider"
        for(field in fields){
            data[field] = ""
        }
        init()
    }

    override fun createCenterPanel(): JComponent {
//        var dialogPanel = JPanel(BorderLayout())
//
//        var label = JLabel("Testing")
//        label.setPreferredSize(Dimension(100, 100))
//        dialogPanel.add(label, BorderLayout.CENTER)
//
//        return dialogPanel
        return panel {
//            row("Slice Provider Name:") {
//                textField().bindText(::name)
//            }
//            row("Slice Provider Location:") {
//                textField().bindText(::location)
//            }
            var first = true
            for (field in fields){
                row("$field: "){
                    val text = textField()
                        .bindText({ data.getOrDefault(field, "") }, {text -> data[field] = text})
                        .validation { input ->
                            if(input.text.isBlank()){
                                return@validation ValidationInfo("Cannot be empty")
                            }
                            return@validation null
                        }
                    if(first){
                        text.focused()
                        first = false
                    }
                }
            }
        }
    }
}
