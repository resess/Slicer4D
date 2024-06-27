package ca.ubc.ece.resess.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.stepsProcessing.step

@FixtureName("EditorComponentImpl")
@DefaultXpath("EditorComponentImpl type", "//div[@accessiblename.key='editor.for.file.accessible.name']")
class EditorComponentImplFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) :
    CommonContainerFixture(remoteRobot, remoteComponent) {

    val greyedOutLines
        get() = step("Get greyed out lines") {
            return@step callJs<HashSet<Int>>(
                """
                    const pluginId = com.intellij.openapi.extensions.PluginId.getId("ca.ubc.ece.resess");
                    const pluginClassLoader = com.intellij.ide.plugins.PluginManagerCore.getPlugin(pluginId).getPluginClassLoader();
                    const javaUtils = java.lang.Class.forName("ca.ubc.ece.resess.ui.EditorSliceVisualizer", true, pluginClassLoader);
                    javaUtils.getMethod("getGreyedOutLines", com.intellij.openapi.editor.Editor).invoke(null, component.editor)
                """, true
            )
        }
}