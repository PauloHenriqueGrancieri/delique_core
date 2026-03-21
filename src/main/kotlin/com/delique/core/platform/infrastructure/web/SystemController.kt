package com.delique.core.platform.infrastructure.web

import com.delique.core.analytics.application.BusinessAnalyticsApplicationService
import com.delique.core.inventory.application.StockReconciliationResult
import com.delique.core.inventory.application.StockReconciliationService
import com.delique.core.platform.backup.BackupApplicationService
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/system")
@CrossOrigin(origins = ["http://localhost:3000"])
class SystemController(
    private val backupApplicationService: BackupApplicationService,
    private val applicationContext: ConfigurableApplicationContext,
    private val businessAnalyticsApplicationService: BusinessAnalyticsApplicationService,
    private val stockReconciliationService: StockReconciliationService,
) {
    @PostMapping("/quit")
    fun quit(): ResponseEntity<Map<String, String>> {
        val backupPath = backupApplicationService.createBackup()

        Thread {
            Thread.sleep(500)
            applicationContext.close()
        }.start()

        return ResponseEntity.ok(
            mapOf(
                "message" to "Backup created. Shutting down...",
                "backupPath" to backupPath,
            ),
        )
    }

    @PostMapping("/business-analytics/run")
    fun runBusinessAnalytics(): ResponseEntity<Map<String, String>> {
        businessAnalyticsApplicationService.recalculateAllMetrics()
        return ResponseEntity.ok(
            mapOf(
                "message" to "Análise recalculada com sucesso. Execute de tempos em tempos para manter a classificação ABC/XYZ e as margens sugeridas atualizadas.",
            ),
        )
    }

    @PostMapping("/stock-reconcile")
    fun stockReconcile(): ResponseEntity<StockReconciliationResult> {
        val result = stockReconciliationService.reconcile()
        return ResponseEntity.ok(result)
    }
}
