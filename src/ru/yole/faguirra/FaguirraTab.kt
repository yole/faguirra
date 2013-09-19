package ru.yole.faguirra

import java.awt.BorderLayout
import com.intellij.openapi.ui.Splitter
import javax.swing.JPanel
import javax.swing.JComponent
import org.jetbrains.plugins.terminal.OpenLocalTerminalAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.intellij.openapi.wm.IdeFocusManager

public class FaguirraTab(val project: Project): JPanel(BorderLayout()), Disposable {
    val leftPanel = FaguirraPanel(project, this)
    val rightPanel = FaguirraPanel(project, this)
    val splitter = Splitter(true, 0.8)
    val panelSplitter = Splitter(false)
    var rightPanelActive = false
    val terminalRunner = OpenLocalTerminalAction.createTerminalRunner(project)!!;

    {
        panelSplitter.setFirstComponent(leftPanel)
        panelSplitter.setSecondComponent(rightPanel)
        splitter.setFirstComponent(panelSplitter)
        val widget = terminalRunner.createTerminalWidget()!!
        splitter.setSecondComponent(widget)
        add(splitter, BorderLayout.CENTER)

    }

    public fun getPreferredFocusComponent(): JComponent = if (rightPanelActive) rightPanel else leftPanel

    public override fun dispose() {
    }

    public fun getState(): FaguirraEditorState {
      return FaguirraEditorState(leftPanel.getState(), rightPanel.getState(), false)
    }

    public fun setState(state: FaguirraEditorState) {
        leftPanel.setState(state.leftPanelState)
        rightPanel.setState(state.rightPanelState)
        rightPanelActive = state.rightPanelActive
    }
}