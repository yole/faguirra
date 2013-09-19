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

public class FaguirraPanel(val project: Project): JPanel(BorderLayout()), DataProvider {
    private val fileListModel = CollectionListModel<VirtualFile>()
    private val fileList = JList(fileListModel)
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
        fileList.setCellRenderer(FileRenderer(this))
        add(fileList, BorderLayout.CENTER)
        updateCurrentDir(null)
    }

    private fun updateCurrentDir(fileToSelect: VirtualFile?) {
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
        val selectedFile = selection[0] as? VirtualFile
        val fileToSelect: VirtualFile?
        if (selectedFile == null) {
            val parent = currentDir.getParent()
            if (parent == null) return
            fileToSelect = currentDir
            currentDir = parent
        }
        else {
            if (!selectedFile.isDirectory()) return
            fileToSelect = null
            currentDir = selectedFile
        }
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

    override fun getData(dataKey: String?): Any? =
            when(dataKey) {
                PlatformDataKeys.VIRTUAL_FILE_ARRAY.getName() -> getSelectedFiles()
                LangDataKeys.PSI_ELEMENT_ARRAY.getName() -> getSelectedPsiFiles()
                PlatformDataKeys.PROJECT.getName() -> project
                else -> null
            }

    public fun getPreferredFocusComponent(): JComponent? = fileList
}
