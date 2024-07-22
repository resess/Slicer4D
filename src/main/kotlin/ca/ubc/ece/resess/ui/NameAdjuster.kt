package ca.ubc.ece.resess.ui

import com.intellij.debugger.ui.tree.NodeDescriptor
import com.intellij.debugger.ui.tree.NodeDescriptorNameAdjuster

class NameAdjuster : NodeDescriptorNameAdjuster() {
    override fun isApplicable(nodeDescriptor: NodeDescriptor): Boolean {
        return true
    }

    override fun fixName(s: String, nodeDescriptor: NodeDescriptor): String {
        return s
    }
}