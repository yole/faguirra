package ru.yole.faguirra

import java.awt.BorderLayout
import com.intellij.openapi.ui.Splitter
import javax.swing.JPanel
import javax.swing.JComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import com.pty4j.PtyProcess
import com.intellij.execution.process.ProcessHandler
import java.io.OutputStream
import com.jediterm.terminal.TtyConnector

class FaguirraTerminalRunner(project: Project): LocalTerminalDirectRunner(project) {
    var ttyConnector: TtyConnector? = null

    override fun createTtyConnector(process: PtyProcess?): TtyConnector? {
        ttyConnector = super<LocalTerminalDirectRunner>.createTtyConnector(process)
        return ttyConnector
    }
}

public class FaguirraTab(val project: Project): JPanel(BorderLayout()), Disposable {
    val leftPanel = FaguirraPanel(project, this)
    val rightPanel = FaguirraPanel(project, this)
    val splitter = Splitter(true, 0.8)
    val panelSplitter = Splitter(false)
    var rightPanelActive = false
    val terminalRunner = FaguirraTerminalRunner(project);

    {
        leftPanel.directoryChangeListeners.add({currentDirChanged(it)})
        rightPanel.directoryChangeListeners.add({currentDirChanged(it)})
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

    private fun currentDirChanged(dir: VirtualFile) {
        val input = terminalRunner.ttyConnector
        if (input != null) {
            input.write("cd " + dir.getPath() + "\n")
        }
    }

    public fun getState(): FaguirraEditorState {
      return FaguirraEditorState(leftPanel.getState(), rightPanel.getState(), false)
    }

    public fun setState(state: FaguirraEditorState) {
        leftPanel.setState(state.leftPanelState)
        rightPanel.setState(state.rightPanelState)
        rightPanelActive = state.rightPanelActive
    }

    public fun getOppositePanel(panel: FaguirraPanel): FaguirraPanel =
            when(panel) {
                leftPanel -> rightPanel
                rightPanel -> leftPanel
                else -> throw IllegalArgumentException("panel from wrong tab")
            }
}