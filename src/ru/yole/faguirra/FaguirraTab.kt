package ru.yole.faguirra

import java.awt.BorderLayout
import com.intellij.openapi.ui.Splitter
import javax.swing.JPanel
import javax.swing.JComponent
import org.jetbrains.plugins.terminal.OpenLocalTerminalAction
import com.intellij.openapi.project.Project

public class FaguirraTab(val project: Project): JPanel(BorderLayout()) {
    val leftPanel = FaguirraPanel()
    val rightPanel = FaguirraPanel()
    val splitter = Splitter(true, 0.8)
    val panelSplitter = Splitter(false)
    val terminalRunner = OpenLocalTerminalAction.createTerminalRunner(project)!!;

    {
        panelSplitter.setFirstComponent(FaguirraPanel())
        panelSplitter.setSecondComponent(FaguirraPanel())
        splitter.setFirstComponent(panelSplitter)
        val widget = terminalRunner.createTerminalWidget()!!
        splitter.setSecondComponent(widget)
        add(splitter, BorderLayout.CENTER)

    }

    public fun getPreferredFocusComponent(): JComponent = leftPanel
}