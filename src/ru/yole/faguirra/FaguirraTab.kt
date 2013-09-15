package ru.yole.faguirra

import java.awt.BorderLayout
import com.intellij.openapi.ui.Splitter
import javax.swing.JPanel
import javax.swing.JComponent

public class FaguirraTab(): JPanel(BorderLayout()) {
    val leftPanel = FaguirraPanel()
    val rightPanel = FaguirraPanel()
    val splitter = Splitter(false);

    {
        splitter.setFirstComponent(FaguirraPanel())
        splitter.setSecondComponent(FaguirraPanel())
        add(splitter, BorderLayout.CENTER)
    }

    public fun getPreferredFocusComponent(): JComponent = leftPanel
}