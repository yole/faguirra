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
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.ide.IdeView
import com.intellij.psi.PsiFileSystemItem
import com.intellij.openapi.fileChooser.actions.VirtualFileDeleteProvider
import com.intellij.util.ui.UIUtil
import java.util.ArrayList
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.util.text.DateFormatUtil
import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.vfs.JarFileSystem

public class FileRenderer(val panel: FaguirraPanel): ColoredListCellRenderer() {
    override fun customizeCellRenderer(list: JList?, value: Any?, index: Int, selected: Boolean, hasFocus: Boolean) {
        if (selected) {
            setBackground(if (hasFocus) UIUtil.getListSelectionBackground() else UIUtil.getListUnfocusedSelectionBackground())
        }
        mySelectionForeground = if (hasFocus) UIUtil.getListSelectionForeground() else UIUtil.getListForeground()

        val file = value as VirtualFile
        if (file == panel.currentParent) {
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
    private fun isRelevant(event: VFileEvent?): Boolean {
        if (event?.getFile()?.getParent() == panel.currentDir) return true
        if (event is VFileMoveEvent) {
            return event.getOldParent() == panel.currentDir || event.getNewParent() == panel.currentDir
        }
        if (event is VFileCopyEvent) {
            return event.getNewParent() == panel.currentDir
        }
        return false
    }

    override fun after(events: List<VFileEvent?>) {
        if (events.any { isRelevant(it) }) {
            panel.refreshCurrentDir()
        }
    }
}

public class FocusOppositePanelAction(val panel: FaguirraPanel): AnAction() {
    override fun actionPerformed(e: AnActionEvent?) {
        val oppositePanel = panel.tab.getOppositePanel(panel)
        IdeFocusManager.getInstance(panel.project)!!.requestFocus(oppositePanel.getPreferredFocusComponent(), false)
    }
}

public class FocusTerminalAction(val panel: FaguirraPanel): AnAction() {
    override fun actionPerformed(e: AnActionEvent?) {
        val focusTarget = panel.tab.terminalWidget?.getFocusableComponent()
        if (focusTarget != null) {
            IdeFocusManager.getInstance(panel.project)!!.requestFocus(focusTarget, false)
        }
    }
}

public class FaguirraPanel(val project: Project, val tab: FaguirraTab)
        : JPanel(BorderLayout()), DataProvider, Disposable, IdeView {
    private val fileListModel = CollectionListModel<VirtualFile>()
    private val fileList = JList(fileListModel)
    private val statusLine = JLabel()
    public var currentDir: VirtualFile = LocalFileSystem.getInstance()!!.getRoot()
    public var currentParent: VirtualFile? = null
    public val directoryChangeListeners: ArrayList<Function2<FaguirraPanel, VirtualFile, Unit>> =
            arrayListOf<Function2<FaguirraPanel, VirtualFile, Unit>>()

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

        fileList.addFocusListener(object: FocusAdapter() {
            override fun focusGained(p0: FocusEvent) {
                notifyDirectoryChange()
            }
        })

        val editSourceAction = ActionManager.getInstance()!!.getAction(IdeActions.ACTION_EDIT_SOURCE)
        editSourceAction?.registerCustomShortcutSet(CommonShortcuts.ENTER, fileList)
        val tabShortcut = CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)!!)
        FocusOppositePanelAction(this).registerCustomShortcutSet(tabShortcut, fileList)
        FocusTerminalAction(this).registerCustomShortcutSet(CommonShortcuts.ESCAPE, fileList)

        fileList.setCellRenderer(FileRenderer(this))
        add(JBScrollPane(fileList), BorderLayout.CENTER)

        statusLine.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM + SideBorder.TOP))
        add(statusLine, BorderLayout.SOUTH)

        updateCurrentDir(null)

        project.getMessageBus().connect(this).subscribe(VirtualFileManager.VFS_CHANGES, FaguirraVFSListener(this))
        Disposer.register(tab, this)
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
        val index = if (indexToSelect < 0) 0 else indexToSelect
        fileList.setSelectedIndex(index)
        val bounds = fileList.getCellBounds(index, index)
        if (bounds != null) {
            fileList.scrollRectToVisible(bounds)
        }
    }

    private fun getDirContents(dir: VirtualFile): List<VirtualFile> {
        val result = arrayListOf<VirtualFile>()
        dir.getChildren()!!.toCollection(result)
        Collections.sort(result, object: Comparator<VirtualFile> {
            override fun compare(p0: VirtualFile, p1: VirtualFile): Int {
                return p0.getName().toLowerCase().compareTo(p1.getName().toLowerCase())
            }
        })
        val parent = currentParent
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

    private fun getLocalFile(f: VirtualFile): VirtualFile {
        if (f.getFileSystem() is JarFileSystem) {
            val localFile = JarFileSystem.getInstance()!!.getLocalVirtualFileFor(f)
            if (localFile != null) return localFile
        }
        return f
    }

    public fun changeDir(dir: VirtualFile) {
        var fileToSelect: VirtualFile? = null
        if (dir == currentParent) {
            fileToSelect = if (currentDir.getParent() == null) getLocalFile(currentDir) else currentDir
        }
        if (!dir.isDirectory()) {
            if (dir.getFileType() !is ArchiveFileType) return
            val jarRoot = JarFileSystem.getInstance()!!.getJarRootForLocalFile(dir)
            if (jarRoot == null) return
            currentParent = dir.getParent()
            currentDir = jarRoot
        }
        else {
            currentDir = dir
            currentParent = currentDir.getParent()
            if (currentParent == null && currentDir.getFileSystem() is JarFileSystem) {
                currentParent = getLocalFile(currentDir).getParent()
            }
        }
        dir.refresh(true, false)
        updateCurrentDir(fileToSelect)
        notifyDirectoryChange()
    }

    private fun notifyDirectoryChange() {
        directoryChangeListeners.forEach { it(this, currentDir) }
    }

    private fun getSelectedFiles(includeParent: Boolean = false): Array<VirtualFile> {
        val selectionArray = fileList.getSelectedValues() as Array<Any?>
        val selection = selectionArray.filter { it as? VirtualFile != null && (includeParent || it != currentParent) }
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
        val navigatableList = getSelectedFiles(true).map {
            if (it.isDirectory() || it.getFileType() is ArchiveFileType)
                PanelNavigatable(this, it)
            else
                OpenFileDescriptor(project, it)
        }
        return navigatableList.toArray(arrayOfNulls<Navigatable>(navigatableList.size())) as Array<Navigatable>
    }

    private fun getTargetPsiElement() =
        PsiManager.getInstance(project).findDirectory(tab.getOppositePanel(this).currentDir)

    private fun updateStatusLine() {
        val text = StringBuilder(" ")
        val selection = getSelectedFiles()
        if (selection.size != 1) {
            val selectedFiles = selection.filter { !it.isDirectory() }
            val selectedDirs = selection.filter { it.isDirectory() }
            if (selectedDirs.size() > 0) {
                text.append(selectedDirs.size).append(" directories");
                if (selectedFiles.size > 0) {
                    text.append(" and ").append(selectedFiles.size).append(" files")
                }
                text.append(" selected")
            }
            else if (selectedFiles.size > 0) {
                text.append(selectedFiles.size()).append(" files selected, size ")
                val totalSize = selectedFiles.fold(0.toLong(), {(size, file) -> size + file.getLength()})
                text.append(StringUtil.formatFileSize(totalSize))
            }
            else {
                text.append("No selection")
            }
        }
        else if (!selection[0].isDirectory()) {
            text.append(StringUtil.formatFileSize(selection[0].getLength()))
            text.append(", modified on ").append(DateFormatUtil.formatDateTime(selection[0].getTimeStamp()))
        }
        else {
            text.append("1 directory selected")
        }
        statusLine.setText(text.toString())
    }

    override fun getData(dataKey: String?): Any? =
            when(dataKey) {
                PlatformDataKeys.VIRTUAL_FILE_ARRAY.getName() -> getSelectedFiles()
                LangDataKeys.PSI_ELEMENT_ARRAY.getName() -> getSelectedPsiFiles()
                PlatformDataKeys.PROJECT.getName() -> project
                PlatformDataKeys.NAVIGATABLE_ARRAY.getName() -> getSelectedNavigatables()
                LangDataKeys.TARGET_PSI_ELEMENT.getName() -> getTargetPsiElement()
                LangDataKeys.IDE_VIEW.getName() -> this
                PlatformDataKeys.DELETE_ELEMENT_PROVIDER.getName() -> VirtualFileDeleteProvider()
                else -> null
            }

    public fun getPreferredFocusComponent(): JComponent = fileList

    public fun getState(): FaguirraPanelState {
        return FaguirraPanelState(getLocalFile(currentDir).getPath())
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

    override fun selectElement(element: PsiElement?) {
        if (element is PsiFileSystemItem) {
            fileList.setSelectedValue(element.getVirtualFile(), true)
        }
    }

    override fun getDirectories() = array(PsiManager.getInstance(project).findDirectory(currentDir)!!)

    override fun getOrChooseDirectory() = PsiManager.getInstance(project).findDirectory(currentDir)
}
