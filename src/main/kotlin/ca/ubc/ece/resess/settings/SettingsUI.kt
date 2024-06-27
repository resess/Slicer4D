package ca.ubc.ece.resess.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.table.AbstractTableModel

class SettingsUI: JPanel() {
    var modified = false
    val settings = service<PluginSettingsStateComponent>()
    var uiSliceProviders = settings.sliceProviders.toMutableList()
    val table = JBTable(object: AbstractTableModel() {
        override fun getRowCount(): Int {
            return uiSliceProviders.size
        }

        override fun getColumnCount(): Int {
            return settings.sliceProviderFields.size
        }

        override fun getColumnName(column: Int): String {
            return settings.sliceProviderFields[column]
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return true
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val key = settings.sliceProviderFields[columnIndex]
            val sliceProviderInfo = uiSliceProviders[rowIndex]
            val result = sliceProviderInfo[key]
            if(result == null){
                return ""
            }
            return result
        }
    })
    private val toolbarDecorator = ToolbarDecorator.createDecorator(table)

    fun createComponent(): JComponent {
        toolbarDecorator.setAddAction {
            val dialogWrapper = AddSliceProviderDialog(settings.sliceProviderFields)
            val result = dialogWrapper.showAndGet()
            if(!result){
                return@setAddAction
            }
            uiSliceProviders.add(dialogWrapper.data)
            table.updateUI()
            modified = true

        }
        toolbarDecorator.setRemoveAction(object: AnActionButtonRunnable{
            override fun run(t: AnActionButton?) {
                val selectedRow = table.selectedRow
                if(selectedRow == -1){
                    return
                }
                uiSliceProviders.removeAt(selectedRow)
                table.updateUI()
                modified = true
            }
        })

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
//            add(JLabel("My Setting:"))
//            add(Breadcrumbs())
            add(toolbarDecorator.createPanel())
        }
//        return panel {
//            row{
//                Breadcrumbs()
//            }
//            row {
//                add(list.createPanel())
//            }
//        }
    }

    fun isModified(): Boolean {
        return modified
    }

    fun apply() {
        modified = false
        settings.sliceProviders = uiSliceProviders.toMutableList()
        table.updateUI()
    }

    fun reset() {
        // Implement logic to reset the settings
        modified = false
        uiSliceProviders = settings.sliceProviders.toMutableList()
        table.updateUI()
    }

    fun dispose() {
        // Implement logic to dispose resources
    }
}

class AddSliceProviderDialog : DialogWrapper {
//    var name: String = ""
//    var location: String = ""
//    var url: String = ""
    var fields: List<String> = listOf()
    var data = HashMap<String, String>()

    constructor(fields: List<String>) : super(true) {
        this.fields = fields
        title = "Add Slice Provider"
        for(field in fields){
            data[field] = ""
        }
        init()
    }

    override fun createCenterPanel(): JComponent? {
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
//            row("Slice Provider URL") {
//                textField().bindText(::url)
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
