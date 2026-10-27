/*
* Copyright 2024 Dmytro Ponomarenko
* Modified in 2026 by the R-Recorder fork.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.dimowner.audiorecorder.v2.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.dimowner.audiorecorder.AppConstants
import com.dimowner.audiorecorder.exception.CantCreateFileException
import com.dimowner.audiorecorder.v2.data.extensions.createFile
import com.dimowner.audiorecorder.v2.data.extensions.deleteFileAndChildren
import com.dimowner.audiorecorder.v2.data.extensions.getPrivateMusicStorageDir
import com.dimowner.audiorecorder.v2.data.extensions.markFileAsDeleted
import com.dimowner.audiorecorder.v2.data.extensions.renameFileWithExtension
import com.dimowner.audiorecorder.v2.data.extensions.requestAllocateSpace
import com.dimowner.audiorecorder.v2.data.extensions.unmarkFileAsDeleted
import com.dimowner.audiorecorder.v2.data.model.StorageLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Name of the app's folder inside the shared Recordings folder. */
private const val PUBLIC_DIR_NAME = "R-Recorder"

@Singleton
class FileDataSourceImpl @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsV2,
): FileDataSource {

    private val privateRecordDirectory: File? by lazy {
        getPrivateMusicStorageDir(context, AppConstants.RECORDS_DIR)
    }

    override fun getRecordingDir(): File? {
        return when (prefs.storageLocation) {
            StorageLocation.PUBLIC -> getPublicRecordingDir() ?: privateRecordDirectory
            // A picked folder gets a copy once the recording is finished, see copyToCustomFolder.
            StorageLocation.PRIVATE, StorageLocation.CUSTOM -> privateRecordDirectory
        }
    }

    override fun getPrivateRecordingDir(): File? {
        return privateRecordDirectory
    }

    override fun getPublicRecordingDir(): File? {
        // Since Android 11 an app may create files in the shared media folders through plain file
        // paths without any permission. Recordings is one of those folders only since Android 12.
        val parentName = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> Environment.DIRECTORY_RECORDINGS
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.DIRECTORY_MUSIC
            else -> return null
        }
        val dir = File(Environment.getExternalStoragePublicDirectory(parentName), PUBLIC_DIR_NAME)
        if (!dir.isDirectory && !dir.mkdirs()) {
            Timber.e("Failed to create public recording directory: ${dir.absolutePath}")
            return null
        }
        return dir
    }

    override fun copyToCustomFolder(file: File): Boolean {
        if (prefs.storageLocation != StorageLocation.CUSTOM) return true
        val treeUri = prefs.customStorageTreeUri?.let { Uri.parse(it) } ?: return false
        return try {
            val resolver = context.contentResolver
            val folderUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri, DocumentsContract.getTreeDocumentId(treeUri)
            )
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                ?: "application/octet-stream"
            val target = DocumentsContract.createDocument(resolver, folderUri, mimeType, file.name)
                ?: return false
            val output = resolver.openOutputStream(target) ?: return false
            output.use { out -> file.inputStream().use { it.copyTo(out) } }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy ${file.name} to the picked folder")
            false
        }
    }

    override fun createRecordFile(fileName: String): File {
        val recordFile = getRecordingDir()?.let {
            try {
                createFile(it, fileName)
            } catch (e: IOException) {
                Timber.e(e, "Failed to create record file with name: $fileName in directory: ${it.absolutePath}")
                throw CantCreateFileException()
            }
        }
        if (recordFile != null) {
            return recordFile
        }
        throw CantCreateFileException()
    }

    override fun deleteRecordFile(path: String): Boolean {
        return deleteFileAndChildren(File(path))
    }

    override fun markAsRecordDeleted(path: String): String? {
        return markFileAsDeleted(File(path))?.absolutePath
    }

    override fun unmarkRecordAsDeleted(path: String): String? {
        return unmarkFileAsDeleted(File(path))?.absolutePath
    }

    override fun renameFile(path: String, newName: String): File? {
        return renameFileWithExtension(File(path), newName)
    }

    @SuppressLint("UsableSpace")
    override fun getAvailableSpace(): Long {
        return getRecordingDir()?.usableSpace ?: 0
    }

    override fun requestSystemMoreMemory(context: Context, file: File, requiredSpace: Long) {
        requestAllocateSpace(context, file, requiredSpace)
    }
}
