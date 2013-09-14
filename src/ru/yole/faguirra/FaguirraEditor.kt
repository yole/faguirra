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
import javax.swing.JComponent
import java.beans.PropertyChangeListener
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.ide.structureView.StructureViewBuilder

public class FaguirraEditorProvider(): FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile) = file is FaguirraVirtualFile

    override fun createEditor(project: Project, file: VirtualFile) = FaguirraFileEditor()
    override fun disposeEditor(editor: FileEditor) { }

    override fun readState(sourceElement: Element, project: Project, file: VirtualFile) = FaguirraEditorState()
    override fun writeState(state: FileEditorState, project: Project, targetElement: Element) { }

    override fun getEditorTypeId() = "Faguirra"
    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

public class FaguirraEditorState(): FileEditorState {
    override fun canBeMergedWith(otherState: FileEditorState?, level: FileEditorStateLevel?) = false
}

public class FaguirraFileEditor(): UserDataHolderBase(), FileEditor, DumbAware {
    private val panel = FaguirraPanel()

    override fun getComponent() = panel
    override fun getPreferredFocusedComponent() = panel.getPreferredFocusComponent()
    override fun getName() = "Faguirra"
    override fun getState(level: FileEditorStateLevel) = FaguirraEditorState()
    override fun setState(state: FileEditorState) { }

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
    }
}
