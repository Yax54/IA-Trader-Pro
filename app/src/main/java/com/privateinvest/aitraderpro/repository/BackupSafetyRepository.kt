package com.privateinvest.aitraderpro.repository

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sauvegarde locale automatique de sécurité.
 *
 * Ce module se branche sur UltimateRepository.exportBackupJson() — unique moteur
 * de sauvegarde de l'application. Il ne duplique aucune logique et ne crée pas
 * un second système de sauvegarde.
 *
 * Principe : la sauvegarde ne bloque jamais le trading. Elle sert uniquement
 * de filet de sécurité avant restauration, migration ou passage au mode réel.
 * La sauvegarde manuelle JSON reste la sauvegarde de référence.
 */
data class LocalBackupSnapshot(
    val fileName: String,
    val absolutePath: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val reason: String
)

class BackupSafetyRepository(private val context: Context) {
    private val backupDir: File by lazy {
        File(context.filesDir, "ai_trader_local_backups").also { it.mkdirs() }
    }

    /**
     * Crée un snapshot local en déléguant à UltimateRepository.exportBackupJson()
     * — seul moteur de sauvegarde de l'application.
     */
    suspend fun createLocalSnapshot(reason: String): LocalBackupSnapshot {
        // Branchement sur le système existant : UltimateRepository.exportBackupJson()
        val backupJson = UltimateRepository().exportBackupJson()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date())
        val safeReason = reason.lowercase(Locale.FRANCE).replace(Regex("[^a-z0-9]+"), "_").trim('_')
        val file = File(backupDir, "auto_backup_${timestamp}_${safeReason.ifBlank { "security" }}.json")
        file.writeText(backupJson)
        return LocalBackupSnapshot(
            fileName = file.name,
            absolutePath = file.absolutePath,
            createdAt = file.lastModified(),
            sizeBytes = file.length(),
            reason = reason
        )
    }

    fun latestLocalSnapshot(): LocalBackupSnapshot? {
        val file = backupDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.maxByOrNull { it.lastModified() }
            ?: return null
        return LocalBackupSnapshot(
            fileName = file.name,
            absolutePath = file.absolutePath,
            createdAt = file.lastModified(),
            sizeBytes = file.length(),
            reason = "Sauvegarde automatique locale"
        )
    }

    fun listSnapshots(): List<LocalBackupSnapshot> = backupDir.listFiles()
        ?.filter { it.isFile && it.name.endsWith(".json") }
        ?.sortedByDescending { it.lastModified() }
        ?.map {
            LocalBackupSnapshot(
                fileName = it.name,
                absolutePath = it.absolutePath,
                createdAt = it.lastModified(),
                sizeBytes = it.length(),
                reason = "Sauvegarde automatique locale"
            )
        }
        ?: emptyList()
}
