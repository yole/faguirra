package ru.yole.faguirra.actions

import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesHandler
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDirectory

public class FaguirraMoveHandler(): MoveFilesOrDirectoriesHandler() {
     override fun isValidTarget(psiElement: PsiElement?, sources: Array<out PsiElement>?): Boolean =
        psiElement is PsiDirectory
}
