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

public class FileRenderer(): ColoredListCellRenderer() {
    override fun customizeCellRenderer(p0: JList?, p1: Any?, p2: Int, p3: Boolean, p4: Boolean) {
        val file = p1 as VirtualFile?
        if (file == null) {
            append("..")
        }
        else {
            append(file.getName())
            setIcon(IconUtil.getIcon(file, 0, null))
        }
    }
}

public class FaguirraPanel(): JPanel(BorderLayout()) {
    private val fileListModel = CollectionListModel<VirtualFile?>()
    private var fileList: JList? = null
    private var currentDir: VirtualFile? = null
    private var showHiddenFiles: Boolean = false

    {
        fileList = JList(fileListModel)
        fileList!!.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.getClickCount() == 2 && event.getButton() == 1) {
                    gotoSelectedDir();
                }
            }
        })
        fileList!!.setCellRenderer(FileRenderer())
        add(fileList!!, BorderLayout.CENTER)
        currentDir = LocalFileSystem.getInstance()!!.findFileByPath("/")
        updateCurrentDir(null)
    }

    private fun updateCurrentDir(fileToSelect: VirtualFile?) {
        if (currentDir == null) {
            fileListModel.replaceAll(listOf())
        }
        else {
            val contents = getDirContents(currentDir!!)
            fileListModel.replaceAll(contents)
            val indexToSelect = contents.indexOf(fileToSelect)
            fileList!!.setSelectedIndex(if (indexToSelect < 0) 0 else indexToSelect)
        }
    }

    private fun getDirContents(dir: VirtualFile): List<VirtualFile?> {
        val result = arrayListOf<VirtualFile?>()
        dir.getChildren()!!.toCollection(result)
        result.sortBy { it -> it!!.getName().toLowerCase() }
        if (dir.getParent() != null) {
            result.add(0, null)
        }
        if (!showHiddenFiles) {
            return result.filter { it -> it == null || !it.getName().startsWith(".") }
        }
        return result
    }

    private fun gotoSelectedDir() {
        val selection = fileList!!.getSelectedValues()
        if (selection.size == 0) return
        val selectedFile = selection[0] as? VirtualFile
        val fileToSelect: VirtualFile?
        if (selectedFile == null) {
            fileToSelect = currentDir
            currentDir = currentDir?.getParent()
        }
        else {
            fileToSelect = null
            currentDir = selectedFile
        }
        updateCurrentDir(fileToSelect)
    }

    public fun getPreferredFocusComponent(): JComponent? = fileList
}
