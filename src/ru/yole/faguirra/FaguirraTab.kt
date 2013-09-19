package ru.yole.faguirra

import java.awt.BorderLayout
import com.intellij.openapi.ui.Splitter
import javax.swing.JPanel
import javax.swing.JComponent
import org.jetbrains.plugins.terminal.OpenLocalTerminalAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable

public class FaguirraTab(val project: Project): JPanel(BorderLayout()), Disposable {
    val leftPanel = FaguirraPanel(project, this)
    val rightPanel = FaguirraPanel(project, this)
    val splitter = Splitter(true, 0.8)
    val panelSplitter = Splitter(false)
    val terminalRunner = OpenLocalTerminalAction.createTerminalRunner(project)!!;

    {
        panelSplitter.setFirstComponent(leftPanel)
        panelSplitter.setSecondComponent(rightPanel)
        splitter.setFirstComponent(panelSplitter)
        val widget = terminalRunner.createTerminalWidget()!!
        splitter.setSecondComponent(widget)
        add(splitter, BorderLayout.CENTER)

    }

    public fun getPreferredFocusComponent(): JComponent = leftPanel

    public override fun dispose() {
    }
}