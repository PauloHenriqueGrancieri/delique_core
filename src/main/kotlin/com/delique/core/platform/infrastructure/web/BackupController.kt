package com.delique.core.platform.infrastructure.web

import com.delique.core.platform.backup.BackupApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/backup")
@CrossOrigin(origins = ["http://localhost:3000"])
class BackupController(
    private val backupApplicationService: BackupApplicationService,
) {
    @PostMapping("/create")
    fun createBackup(): ResponseEntity<Map<String, String>> {
        val backupPath = backupApplicationService.createBackup()
        return ResponseEntity.ok(
            mapOf(
                "message" to "Backup created successfully",
                "path" to backupPath,
            ),
        )
    }
}
