package ca.ubc.ece.resess.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class SlicerConfigurable: Configurable {
    private val settingsPanel = SettingsUI()

    override fun createComponent(): JComponent{
        return settingsPanel.createComponent()
    }

    override fun isModified(): Boolean = settingsPanel.isModified()

    override fun apply() {
        settingsPanel.apply()
    }

    override fun reset() {
        settingsPanel.reset()
    }

    override fun disposeUIResources() {
        settingsPanel.dispose()
    }

    override fun getDisplayName(): String = "Slicer Configuration Menu"
}