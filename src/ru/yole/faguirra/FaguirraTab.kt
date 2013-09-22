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
import com.jediterm.terminal.TtyConnector
import org.jetbrains.plugins.terminal.JBTabbedTerminalWidget
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.jediterm.terminal.ui.TerminalActionProvider
import com.jediterm.terminal.ui.TerminalAction
import javax.swing.KeyStroke
import java.awt.event.KeyEvent

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
    var lastActivePanel: FaguirraPanel = leftPanel
    var terminalWidget: JBTabbedTerminalWidget? = null
    val terminalRunner = FaguirraTerminalRunner(project);

    {
        val handler: (FaguirraPanel, VirtualFile) -> Unit = {(panel, dir) -> currentDirChanged(panel, dir) }
        leftPanel.directoryChangeListeners.add(handler)
        rightPanel.directoryChangeListeners.add(handler)
        panelSplitter.setFirstComponent(leftPanel)
        panelSplitter.setSecondComponent(rightPanel)
        splitter.setFirstComponent(panelSplitter)
        terminalWidget = terminalRunner.createTerminalWidget()!!
        splitter.setSecondComponent(terminalWidget)
        add(splitter, BorderLayout.CENTER)

        val focusPanelsAction = TerminalAction("Focus panels",
                array(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)!!), {
                    val focusTarget = lastActivePanel.getPreferredFocusComponent()
                    IdeFocusManager.getInstance(project)!!.requestFocus(focusTarget, false)
                    true
                })

        terminalWidget!!.setNextProvider(object: TerminalActionProvider {
            private var myNextProvider: TerminalActionProvider? = null

            override fun getActions() = arrayListOf(focusPanelsAction)
            override fun getNextProvider() = myNextProvider
            override fun setNextProvider(p0: TerminalActionProvider?) { myNextProvider = p0 }
        })
    }

    public fun getPreferredFocusComponent(): JComponent = lastActivePanel

    public override fun dispose() {
    }

    private fun currentDirChanged(panel: FaguirraPanel, dir: VirtualFile) {
        lastActivePanel = panel
        val input = terminalRunner.ttyConnector
        val path = dir.getPath()
        if (input != null && path != null) {
            input.write("cd " + path.replace(" ", "\\ ").replace("(", "\\(").replace(")", "\\)") + "\n")
        }
    }

    public fun getState(): FaguirraEditorState {
      return FaguirraEditorState(leftPanel.getState(), rightPanel.getState(), false)
    }

    public fun setState(state: FaguirraEditorState) {
        leftPanel.setState(state.leftPanelState)
        rightPanel.setState(state.rightPanelState)
        lastActivePanel = if (state.rightPanelActive) rightPanel else leftPanel
    }

    public val rightPanelActive: Boolean
        get() = lastActivePanel == rightPanel

    public fun getOppositePanel(panel: FaguirraPanel): FaguirraPanel =
            when(panel) {
                leftPanel -> rightPanel
                rightPanel -> leftPanel
                else -> throw IllegalArgumentException("panel from wrong tab")
            }
}