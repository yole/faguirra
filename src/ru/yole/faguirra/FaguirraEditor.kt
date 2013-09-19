package ru.yole.faguirra

import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.fileEditor.FileEditor
import org.jdom.Element
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorPolicy
import ru.yole.faguirra.fs.FaguirraVirtualFile
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.util.UserDataHolderBase
import java.beans.PropertyChangeListener
import com.intellij.openapi.util.Disposer

public data class FaguirraPanelState(val currentDir: String?) {
    fun writeState(targetElement: Element) {
        if (currentDir != null) {
            targetElement.setAttribute("currentDir", currentDir)
        }
    }

    class object {
        fun read(sourceElement: Element?): FaguirraPanelState {
            val currentDir = sourceElement?.getAttributeValue("currentDir")
            return FaguirraPanelState(currentDir)
        }
    }
}

public class FaguirraEditorState(val leftPanelState: FaguirraPanelState,
                                 val rightPanelState: FaguirraPanelState,
                                 val rightPanelActive: Boolean): FileEditorState {
    override fun canBeMergedWith(otherState: FileEditorState?, level: FileEditorStateLevel?) = false
}

public class FaguirraEditorProvider(): FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile) = file is FaguirraVirtualFile

    override fun createEditor(project: Project, file: VirtualFile) = FaguirraFileEditor(project)
    override fun disposeEditor(editor: FileEditor) { }

    override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FaguirraEditorState {
        val leftPanelState = FaguirraPanelState.read(sourceElement.getChild("left"))
        val rightPanelState = FaguirraPanelState.read(sourceElement.getChild("right"))
        return FaguirraEditorState(leftPanelState, rightPanelState, false)
    }

    override fun writeState(state: FileEditorState, project: Project, targetElement: Element) {
        if (state is FaguirraEditorState) {
            val left = Element("left")
            state.leftPanelState.writeState(left)
            targetElement.addContent(left)

            val right = Element("right")
            state.rightPanelState.writeState(right)
            targetElement.addContent(right)
        }
    }

    override fun getEditorTypeId() = "Faguirra"
    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

public class FaguirraFileEditor(val project: Project): UserDataHolderBase(), FileEditor, DumbAware {
    private val tab = FaguirraTab(project)

    override fun getComponent() = tab
    override fun getPreferredFocusedComponent() = tab.getPreferredFocusComponent()
    override fun getName() = "Faguirra"

    override fun getState(level: FileEditorStateLevel) = tab.getState()
    override fun setState(state: FileEditorState) {
        if (state is FaguirraEditorState) {
            tab.setState(state)
        }
    }

    override fun isModified() = false
    override fun isValid() = true

    override fun selectNotify() { }
    override fun deselectNotify() { }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
    }
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
    }

    override fun getBackgroundHighlighter() = null
    override fun getCurrentLocation() = null
    override fun getStructureViewBuilder() = null

    override fun dispose() {
        Disposer.dispose(tab)
    }
}
