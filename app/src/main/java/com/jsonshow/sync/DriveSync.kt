package com.jsonshow.sync

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DriveFile(
    val id: String,
    val name: String,
    val modifiedTime: Long
)

class DriveSync(context: Context, account: GoogleSignInAccount) {

    private val drive: Drive

    init {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccount = account.account }

        drive = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("JsonShow").build()
    }

    suspend fun listFiles(): List<DriveFile> = withContext(Dispatchers.IO) {
        val result = drive.files().list()
            .setSpaces("appDataFolder")
            .setFields("files(id, name, modifiedTime)")
            .setPageSize(100)
            .execute()
        result.files?.map { f ->
            DriveFile(
                id = f.id,
                name = f.name,
                modifiedTime = f.modifiedTime?.value ?: 0L
            )
        } ?: emptyList()
    }

    suspend fun uploadFile(name: String, content: String): String = withContext(Dispatchers.IO) {
        // Check if file already exists
        val existing = listFiles().find { it.name == name }
        if (existing != null) {
            // Update existing file
            val media = ByteArrayContent.fromString("application/json", content)
            drive.files().update(existing.id, null, media).execute()
            existing.id
        } else {
            // Create new file
            val metadata = com.google.api.services.drive.model.File().apply {
                this.name = name
                parents = listOf("appDataFolder")
            }
            val media = ByteArrayContent.fromString("application/json", content)
            drive.files().create(metadata, media)
                .setFields("id")
                .execute().id
        }
    }

    suspend fun downloadFile(fileId: String): String = withContext(Dispatchers.IO) {
        val stream = java.io.ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(stream)
        stream.toString("UTF-8")
    }

    suspend fun deleteFile(fileId: String) = withContext(Dispatchers.IO) {
        drive.files().delete(fileId).execute()
    }
}
