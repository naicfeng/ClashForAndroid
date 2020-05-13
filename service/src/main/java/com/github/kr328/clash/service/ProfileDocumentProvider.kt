package com.github.kr328.clash.service

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import com.github.kr328.clash.service.files.ProfilesResolver
import kotlinx.coroutines.runBlocking

class ProfileDocumentProvider : DocumentsProvider() {
    companion object {
        private const val DEFAULT_ROOT_ID = "0"
        private val DEFAULT_DOCUMENT_COLUMNS = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_SIZE,
            Document.COLUMN_FLAGS
        )
        private val DEFAULT_ROOT_COLUMNS = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID
        )
    }

    private val resolver: ProfilesResolver by lazy {
        ProfilesResolver(context!!)
    }

    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val m = ParcelFileDescriptor.parseMode(mode)

        if (m and ParcelFileDescriptor.MODE_READ_ONLY == 0)
            throw UnsupportedOperationException()

        return runBlocking {
            val file = resolver.resolve(resolvePath(documentId ?: ""))
            file.openFile(ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        return runBlocking {
            try {
                val documentPath = parentDocumentId ?: "/"
                val paths = resolvePath(documentPath)
                val file = resolver.resolve(paths)

                MatrixCursor(resolveDocumentProjection(projection)).apply {
                    file.listFiles().forEach {
                        val childPaths = paths + it
                        val child = resolver.resolve(paths + it)

                        newRow().apply {
                            add(Document.COLUMN_DOCUMENT_ID, childPaths.joinToString("/"))
                            add(Document.COLUMN_DISPLAY_NAME, child.name())
                            add(Document.COLUMN_MIME_TYPE, child.mimeType())
                            add(Document.COLUMN_LAST_MODIFIED, child.lastModified())
                            add(Document.COLUMN_SIZE, child.size())
                            add(Document.COLUMN_FLAGS, 0)
                        }
                    }
                }
            } catch (e: Exception) {
                MatrixCursor(resolveDocumentProjection(projection))
            }
        }
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        return runBlocking {
            try {
                val documentPath = documentId ?: "/"
                val paths = resolvePath(documentPath)
                val file = resolver.resolve(paths)

                MatrixCursor(resolveDocumentProjection(projection)).apply {
                    newRow().apply {
                        add(Document.COLUMN_DOCUMENT_ID, documentPath)
                        add(Document.COLUMN_DISPLAY_NAME, file.name())
                        add(Document.COLUMN_MIME_TYPE, file.mimeType())
                        add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
                        add(Document.COLUMN_SIZE, file.size())
                        add(Document.COLUMN_FLAGS, 0)
                    }
                }
            } catch (e: Exception) {
                MatrixCursor(resolveDocumentProjection(projection))
            }
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val flags = Root.FLAG_LOCAL_ONLY or Root.FLAG_SUPPORTS_IS_CHILD

        return MatrixCursor(projection ?: DEFAULT_ROOT_COLUMNS).apply {
            newRow().apply {
                add(Root.COLUMN_ROOT_ID, DEFAULT_ROOT_ID)
                add(Root.COLUMN_FLAGS, flags)
                add(Root.COLUMN_ICON, R.drawable.ic_icon)
                add(Root.COLUMN_TITLE, context!!.getString(R.string.clash_for_android))
                add(Root.COLUMN_SUMMARY, context!!.getString(R.string.profiles_and_providers))
                add(Root.COLUMN_DOCUMENT_ID, "/")
                add(Root.COLUMN_MIME_TYPES, Document.MIME_TYPE_DIR)
            }
        }
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        if (parentDocumentId == null || documentId == null)
            return false

        return documentId.startsWith(parentDocumentId)
    }

    private fun resolveDocumentProjection(projection: Array<out String>?): Array<out String> {
        return projection ?: DEFAULT_DOCUMENT_COLUMNS
    }

    private fun resolvePath(path: String): List<String> {
        return path.split("/").filter(String::isNotBlank)
    }
}