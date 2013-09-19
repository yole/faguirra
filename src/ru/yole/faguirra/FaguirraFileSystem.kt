package ru.yole.faguirra.fs

import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.OutputStream
import java.io.InputStream
import com.intellij.openapi.vfs.VirtualFileManager
import java.util.ArrayList
import com.intellij.openapi.fileTypes.FileType
import javax.swing.Icon

public class FaguirraFileType(): FileType {
    override fun getName() = "Faguirra"
    override fun getDescription() = "Internal file type for Faguirra panels"
    override fun getDefaultExtension() = "faguirra"
    override fun getIcon() = null
    override fun isBinary() = false
    override fun isReadOnly() = true
    override fun getCharset(file: VirtualFile, content: ByteArray?): String? = null

    class object {
        val INSTANCE = FaguirraFileType()
    }
}

public class FaguirraVirtualFile(val fileName: String): VirtualFile() {
    override fun getName() = fileName
    override fun getFileSystem() = FaguirraFileSystem.INSTANCE
    override fun getPath() = "Faguirra"
    override fun isWritable() = false
    override fun isDirectory() = false
    override fun isValid() = true
    override fun getParent() = null
    override fun getChildren(): Array<VirtualFile>? = array()

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        throw UnsupportedOperationException()
    }
    override fun getInputStream(): InputStream? {
        throw UnsupportedOperationException()
    }

    override fun contentsToByteArray() = byteArray()
    override fun getTimeStamp() = 0.toLong()
    override fun getModificationStamp() = 0.toLong()
    override fun getLength() = 0.toLong()
    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
    }

    override fun getFileType() = FaguirraFileType.INSTANCE
}

public class FaguirraFileSystem(): DeprecatedVirtualFileSystem() {
    private val files = ArrayList<FaguirraVirtualFile>()

    public fun allocateFile(): VirtualFile {
        val name = if (files.size() == 0) "Faguirra" else "Faguirra (${files.size})"
        val result = FaguirraVirtualFile(name)
        files.add(result)
        return result
    }

    override fun getProtocol(): String = PROTOCOL

    override fun findFileByPath(path: String): VirtualFile? {
        val result = files.find { it.getName() == path }
        if (result != null) return result
        if (path.equals("Faguirra") && files.size() == 0) {
            return allocateFile()
        }
        return null
    }

    override fun refresh(asynchronous: Boolean) {
    }

    override fun refreshAndFindFileByPath(path: String): VirtualFile? = findFileByPath(path)

    override fun deleteFile(requestor: Any?, vFile: VirtualFile) {
        throw UnsupportedOperationException()
    }

    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {
        throw UnsupportedOperationException()
    }

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
        throw UnsupportedOperationException()
    }

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile? {
        throw UnsupportedOperationException()
    }

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile {
        throw UnsupportedOperationException()
    }
    override fun copyFile(requestor: Any?, virtualFile: VirtualFile, newParent: VirtualFile, copyName: String): VirtualFile? {
        throw UnsupportedOperationException()
    }

    class object {
        val PROTOCOL = "faguirra"
        val INSTANCE = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as FaguirraFileSystem
    }
}