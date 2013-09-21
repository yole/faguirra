package ru.yole.faguirra

import javax.swing.JPanel
import java.awt.BorderLayout
import javax.swing.JList
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CollectionListModel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.util.IconUtil
import javax.swing.JComponent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import java.util.Collections
import java.util.Comparator
import com.intellij.openapi.project.Project
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.pom.Navigatable
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.CommonShortcuts
import javax.swing.JLabel
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.components.JBScrollPane

public class FileRenderer(val panel: FaguirraPanel): ColoredListCellRenderer() {
    override fun customizeCellRenderer(list: JList?, value: Any?, index: Int, selected: Boolean, hasFocus: Boolean) {
        val file = value as VirtualFile
        if (file == panel.currentDir.getParent()) {
            append("..")
        }
        else {
            append(file.getName())
            setIcon(IconUtil.getIcon(file, 0, null))
        }
    }
}

public class PanelNavigatable(val panel: FaguirraPanel, val directory: VirtualFile): Navigatable {
    override fun navigate(requestFocus: Boolean) {
        panel.changeDir(directory)
    }

    override fun canNavigate() = true
    override fun canNavigateToSource() = true
}

public class FaguirraVFSListener(val panel: FaguirraPanel): BulkFileListener.Adapter() {
    override fun after(events: List<VFileEvent?>) {
        if (events.any { it?.getFile()?.getParent() == panel.currentDir }) {
            panel.refreshCurrentDir()
        }
    }
}

public class FaguirraPanel(val project: Project, parentDisposable: Disposable): JPanel(BorderLayout()), DataProvider, Disposable {
    private val fileListModel = CollectionListModel<VirtualFile>()
    private val fileList = JList(fileListModel)
    private val statusLine = JLabel()
    public var currentDir: VirtualFile = LocalFileSystem.getInstance()!!.getRoot()

    private var showHiddenFiles: Boolean = false

    {
        fileList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.getClickCount() == 2 && event.getButton() == 1) {
                    gotoSelectedDir();
                }
            }
        })
        fileList.addListSelectionListener({e -> updateStatusLine()})

        val editSourceAction = ActionManager.getInstance()!!.getAction(IdeActions.ACTION_EDIT_SOURCE)
        editSourceAction?.registerCustomShortcutSet(CommonShortcuts.ENTER, fileList)

        fileList.setCellRenderer(FileRenderer(this))
        add(JBScrollPane(fileList), BorderLayout.CENTER)
        add(statusLine, BorderLayout.SOUTH)
        updateCurrentDir(null)

        project.getMessageBus().connect(this).subscribe(VirtualFileManager.VFS_CHANGES, FaguirraVFSListener(this))
        Disposer.register(parentDisposable, this)
    }

    public override fun dispose() {
    }

    public fun refreshCurrentDir() {
        val oldSelectedIndex = fileList.getSelectedIndex()
        val selection = getSelectedFiles()
        val contents = getDirContents(currentDir)
        fileListModel.replaceAll(contents)
        val newIndices = selection.map { contents.indexOf(it) }.filter { it != -1 }
        if (newIndices.size > 0) {
            val newIndexArray = IntArray(newIndices.size())
            for(i in 0.rangeTo(newIndexArray.size-1)) {
                newIndexArray[i] = newIndices[i]
            }
            fileList.setSelectedIndices(newIndexArray)
        }
        else {
            fileList.setSelectedIndex(oldSelectedIndex)
        }
    }

    public fun updateCurrentDir(fileToSelect: VirtualFile?) {
        val contents = getDirContents(currentDir)
        fileListModel.replaceAll(contents)
        val indexToSelect = contents.indexOf(fileToSelect)
        fileList.setSelectedIndex(if (indexToSelect < 0) 0 else indexToSelect)
    }

    private fun getDirContents(dir: VirtualFile): List<VirtualFile> {
        val result = arrayListOf<VirtualFile>()
        dir.getChildren()!!.toCollection(result)
        Collections.sort(result, object: Comparator<VirtualFile> {
            override fun compare(p0: VirtualFile, p1: VirtualFile): Int {
                return p0.getName().toLowerCase().compareTo(p1.getName().toLowerCase())
            }
        })
        val parent = dir.getParent()
        if (parent != null) {
            result.add(0, parent)
        }
        if (!showHiddenFiles) {
            return result.filter { !it.getName().startsWith(".") }
        }
        return result
    }

    private fun gotoSelectedDir() {
        val selection = fileList.getSelectedValues()
        if (selection.size == 0) return
        val selectedFile = selection[0] as VirtualFile
        changeDir(selectedFile)
    }

    public fun changeDir(dir: VirtualFile) {
        if (!dir.isDirectory()) return
        val fileToSelect = if (dir == currentDir.getParent()) currentDir else null
        currentDir = dir
        updateCurrentDir(fileToSelect)
    }

    private fun getSelectedFiles(): Array<VirtualFile> {
        val selectionArray = fileList.getSelectedValues() as Array<Any?>
        val selection = selectionArray.filter { it as? VirtualFile != null }
        return selection.toArray(arrayOfNulls<VirtualFile>(selection.size)) as Array<VirtualFile>
    }

    private fun getSelectedPsiFiles(): Array<PsiElement> {
        val psiManager = PsiManager.getInstance(project)
        val fileList = getSelectedFiles().map {
            if (it.isDirectory()) psiManager.findDirectory(it) else psiManager.findFile(it)
        }.filter { it != null }
        return fileList.toArray(arrayOfNulls<PsiElement>(fileList.size())) as Array<PsiElement>
    }

    private fun getSelectedNavigatables(): Array<Navigatable> {
        val navigatableList = getSelectedFiles().map {
            if (it.isDirectory()) PanelNavigatable(this, it) else OpenFileDescriptor(project, it)
        }
        return navigatableList.toArray(arrayOfNulls<Navigatable>(navigatableList.size())) as Array<Navigatable>
    }

    private fun updateStatusLine() {
        val text = StringBuilder()
        val selection = getSelectedFiles()
        if (selection.size > 1) {
            text.append(selection.size).append(" files selected")
        }
        else if (selection.size == 1 && !selection[0].isDirectory()) {
            text.append(StringUtil.formatFileSize(selection[0].getLength()))
        }
        statusLine.setText(text.toString())
    }

    override fun getData(dataKey: String?): Any? =
            when(dataKey) {
                PlatformDataKeys.VIRTUAL_FILE_ARRAY.getName() -> getSelectedFiles()
                LangDataKeys.PSI_ELEMENT_ARRAY.getName() -> getSelectedPsiFiles()
                PlatformDataKeys.PROJECT.getName() -> project
                PlatformDataKeys.NAVIGATABLE_ARRAY.getName() -> getSelectedNavigatables()
                else -> null
            }

    public fun getPreferredFocusComponent(): JComponent? = fileList

    public fun getState(): FaguirraPanelState {
        return FaguirraPanelState(currentDir.getPath())
    }

    public fun setState(state: FaguirraPanelState) {
        val dir = state.currentDir
        if (dir != null) {
            val file = LocalFileSystem.getInstance()!!.findFileByPath(dir)
            if (file != null) {
                changeDir(file)
            }
        }
    }
}
