package ru.yole.faguirra.actions

import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesHandler
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDirectory
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.psi.PsiFileSystemItem
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.application.ApplicationManager

public class FaguirraMoveHandler(): MoveFilesOrDirectoriesHandler() {
     override fun isValidTarget(psiElement: PsiElement?, sources: Array<out PsiElement>?): Boolean =
        psiElement is PsiDirectory
}

public class FaguirraRenameHandler(): PsiElementRenameHandler() {
    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        val element = if (elements.size == 1) elements[0] else PsiElementRenameHandler.getElement(dataContext)
        if (element !is PsiFileSystemItem) return
        val newName = Messages.showInputDialog(project, "Enter the name of the file:", "Rename File",
                Messages.getQuestionIcon(), element.getName(), null)
        if (newName == null) return
        val runnable = object: Runnable {
            override fun run() {
                element.setName(newName)
            }
        }
        CommandProcessor.getInstance()!!.executeCommand(project, {
            ApplicationManager.getApplication()!!.runWriteAction(runnable)
        }, "Rename File", null)
    }

    override fun isAvailableOnDataContext(dataContext: DataContext?) = isRenaming(dataContext)

    override fun isRenaming(dataContext: DataContext?): Boolean {
        val psiElements = LangDataKeys.PSI_ELEMENT_ARRAY.getData(dataContext!!)
        return psiElements != null && psiElements.size == 1 && psiElements[0] is PsiFileSystemItem &&
                !psiElements[0].getManager()!!.isInProject(psiElements[0])
    }
}
