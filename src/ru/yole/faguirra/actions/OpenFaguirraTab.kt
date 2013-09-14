package ru.yole.faguirra.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import ru.yole.faguirra.fs.FaguirraFileSystem
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.util.AsyncResult
import com.intellij.openapi.fileEditor.impl.EditorWindow

public class OpenFaguirraAction(): AnAction() {
    override fun actionPerformed(e: AnActionEvent?) {
        val project = e?.getProject()
        if (project != null) {
            val file = FaguirraFileSystem.INSTANCE.allocateFile()
            val mgr = FileEditorManagerEx.getInstanceEx(project)!!
            mgr.openFile(file, true)
        }
    }
}
