package com.delique.core.platform.backup

import com.delique.core.analytics.infrastructure.persistence.ProductClassificationJpa
import com.delique.core.analytics.infrastructure.persistence.ProductMetricsJpa
import com.delique.core.catalog.infrastructure.persistence.CatalogJpa
import com.delique.core.catalog.infrastructure.persistence.CatalogPriceByPaymentMethodJpa
import com.delique.core.catalog.infrastructure.persistence.CatalogSettingsJpa
import com.delique.core.financial.infrastructure.persistence.CashInflowJpa
import com.delique.core.financial.infrastructure.persistence.CashJpa
import com.delique.core.financial.infrastructure.persistence.ExpenseJpa
import com.delique.core.inventory.domain.port.ComboRepository
import com.delique.core.inventory.domain.port.PurchaseOrderRepository
import com.delique.core.inventory.domain.port.StockMovementRepository
import com.delique.core.inventory.infrastructure.persistence.JpaStockUnitExpiryJpa
import com.delique.core.inventory.infrastructure.persistence.JpaStockUnitJpa
import com.delique.core.marketing.infrastructure.persistence.MarketingCampaignJpa
import com.delique.core.platform.backup.dto.BackupCash
import com.delique.core.platform.backup.dto.BackupCashInflow
import com.delique.core.platform.backup.dto.BackupCatalog
import com.delique.core.platform.backup.dto.BackupCatalogPriceByPaymentMethod
import com.delique.core.platform.backup.dto.BackupCatalogSettings
import com.delique.core.platform.backup.dto.BackupCategory
import com.delique.core.platform.backup.dto.BackupClient
import com.delique.core.platform.backup.dto.BackupCombo
import com.delique.core.platform.backup.dto.BackupComboItem
import com.delique.core.platform.backup.dto.BackupCreditCardInstallmentFee
import com.delique.core.platform.backup.dto.BackupData
import com.delique.core.platform.backup.dto.BackupExpense
import com.delique.core.platform.backup.dto.BackupMarketingCampaign
import com.delique.core.platform.backup.dto.BackupMarginStrategy
import com.delique.core.platform.backup.dto.BackupOrder
import com.delique.core.platform.backup.dto.BackupPaymentMethodConfig
import com.delique.core.platform.backup.dto.BackupPendingPriceCalculation
import com.delique.core.platform.backup.dto.BackupPriceCalculationConfig
import com.delique.core.platform.backup.dto.BackupProduct
import com.delique.core.platform.backup.dto.BackupProductClassification
import com.delique.core.platform.backup.dto.BackupProductMetrics
import com.delique.core.platform.backup.dto.BackupProductSupplier
import com.delique.core.platform.backup.dto.BackupProductVariationOption
import com.delique.core.platform.backup.dto.BackupPurchaseOrder
import com.delique.core.platform.backup.dto.BackupPurchaseOrderItem
import com.delique.core.platform.backup.dto.BackupSale
import com.delique.core.platform.backup.dto.BackupSaleReturn
import com.delique.core.platform.backup.dto.BackupSaleReturnItem
import com.delique.core.platform.backup.dto.BackupStockMovement
import com.delique.core.platform.backup.dto.BackupStockUnit
import com.delique.core.platform.backup.dto.BackupStockUnitExpiry
import com.delique.core.platform.backup.dto.BackupSupplier
import com.delique.core.platform.backup.dto.BackupTables
import com.delique.core.platform.backup.dto.BackupBrand
import com.delique.core.pricing.infrastructure.persistence.CreditCardInstallmentFeeJpa
import com.delique.core.pricing.infrastructure.persistence.MarginStrategyJpa
import com.delique.core.pricing.infrastructure.persistence.PaymentMethodConfigJpa
import com.delique.core.pricing.infrastructure.persistence.PendingPriceCalculationJpa
import com.delique.core.pricing.infrastructure.persistence.PriceCalculationConfigJpa
import com.delique.core.product.domain.port.BrandRepository
import com.delique.core.product.domain.port.CategoryRepository
import com.delique.core.product.domain.port.ProductRepository
import com.delique.core.sales.infrastructure.persistence.ClientJpa
import com.delique.core.sales.infrastructure.persistence.CustomerOrderJpa
import com.delique.core.sales.infrastructure.persistence.SaleLineJpa
import com.delique.core.sales.infrastructure.persistence.SaleReturnJpa
import com.delique.core.supplier.domain.port.ProductSupplierRepository
import com.delique.core.supplier.domain.port.SupplierRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Base64

@Service
class BackupApplicationService(
    private val categoryRepository: CategoryRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productSupplierRepository: ProductSupplierRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockUnitJpa: JpaStockUnitJpa,
    private val stockUnitExpiryJpa: JpaStockUnitExpiryJpa,
    private val clientJpa: ClientJpa,
    private val saleLineJpa: SaleLineJpa,
    private val customerOrderJpa: CustomerOrderJpa,
    private val priceCalculationConfigJpa: PriceCalculationConfigJpa,
    private val supplierRepository: SupplierRepository,
    private val catalogJpa: CatalogJpa,
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val cashJpa: CashJpa,
    private val expenseJpa: ExpenseJpa,
    private val cashInflowJpa: CashInflowJpa,
    private val marginStrategyJpa: MarginStrategyJpa,
    private val comboRepository: ComboRepository,
    private val pendingPriceCalculationJpa: PendingPriceCalculationJpa,
    private val productMetricsJpa: ProductMetricsJpa,
    private val productClassificationJpa: ProductClassificationJpa,
    private val paymentMethodConfigJpa: PaymentMethodConfigJpa,
    private val creditCardInstallmentFeeJpa: CreditCardInstallmentFeeJpa,
    private val catalogPriceByPaymentMethodJpa: CatalogPriceByPaymentMethodJpa,
    private val catalogSettingsJpa: CatalogSettingsJpa,
    private val marketingCampaignJpa: MarketingCampaignJpa,
    private val saleReturnJpa: SaleReturnJpa,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val backupDir = File(
        System.getenv("BACKUP_DIR") ?: if (File("/app").exists()) "/app/backups" else "./backups",
    )

    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }

    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }

    @Transactional(readOnly = true)
    fun createBackup(): String {
        val timestamp = LocalDateTime.now()
        val timestampStr = timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val backupFile = File(backupDir, "backup_$timestampStr.json")

        val categories = categoryRepository.findAll().map { c ->
            BackupCategory(id = c.id, name = c.name, has_validity = c.hasValidity, display_id = c.displayId)
        }
        val brands = brandRepository.findAll().map { b ->
            BackupBrand(id = b.id, name = b.name, display_id = b.displayId)
        }
        val products = productRepository.findAll().map { product ->
            val productSuppliers = productSupplierRepository.findByProductId(product.id).map { ps ->
                BackupProductSupplier(
                    id = ps.id,
                    supplier_id = ps.supplier.id,
                    url = ps.url,
                    price = ps.price,
                    out_of_stock_at_supplier = ps.outOfStockAtSupplier,
                )
            }
            BackupProduct(
                id = product.id,
                name = product.name,
                description = product.description,
                sku = product.sku,
                image_url = product.imageUrl,
                image_data_base64 = product.imageData?.let { Base64.getEncoder().encodeToString(it) },
                image_media_type = product.imageMediaType,
                product_suppliers = productSuppliers,
                variation_type = product.variationType,
                variation_options = product.variationOptions.map { opt ->
                    BackupProductVariationOption(
                        id = opt.id,
                        name = opt.name,
                        image_url = opt.imageUrl,
                        image_data_base64 = opt.imageData?.let { Base64.getEncoder().encodeToString(it) },
                        image_media_type = opt.imageMediaType,
                        sku = opt.sku,
                    )
                },
                category_id = product.category.id,
                brand_id = product.brand.id,
                display_id = product.displayId,
                minimum_stock = product.minimumStock,
            )
        }
        val pos = purchaseOrderRepository.findAllWithItemsDesc()
        val purchaseOrders = pos.map { o ->
            BackupPurchaseOrder(
                id = o.id,
                total_freight = o.totalFreight,
                status = o.status.name,
                supplier_id = o.supplier?.id,
                created_at = o.createdAt,
                delivered_at = o.deliveredAt,
                cancelled_at = o.cancelledAt,
            )
        }
        val purchaseOrderItems = pos.flatMap { o ->
            o.items.map { i ->
                BackupPurchaseOrderItem(
                    id = i.id,
                    purchase_order_id = o.id,
                    product_id = i.product.id,
                    product_name = i.product.name,
                    quantity = i.quantity,
                    unit_cost = i.unitCost,
                    expires_at = i.expiresAt?.toString(),
                    received_quantity = i.receivedQuantity,
                    variation_option_id = i.variationOption?.id,
                )
            }
        }

        val stockMovements = stockMovementRepository.findAllList().map { movement ->
            BackupStockMovement(
                id = movement.id,
                product_id = movement.product.id,
                product_name = movement.product.name,
                quantity = movement.quantity,
                type = movement.type.name,
                details = movement.details,
                created_at = movement.createdAt,
                purchase_price = movement.purchasePrice,
                expires_at = movement.expiresAt?.toString(),
                sale_id = movement.saleId,
                edited_at = movement.editedAt,
                edit_reason = movement.editReason,
                variation_option_id = movement.variationOption?.id,
            )
        }

        val stockUnits = stockUnitJpa.findAll().map { u ->
            BackupStockUnit(
                id = u.id,
                stock_movement_id = u.stockMovement.id,
                unit_index = u.unitIndex,
            )
        }
        val stockUnitExpiries = stockUnitExpiryJpa.findAll().map { e ->
            BackupStockUnitExpiry(
                stock_movement_id = e.id.stockMovementId,
                unit_index = e.id.unitIndex,
                expires_at = e.expiresAt.toString(),
            )
        }
        val pendingPriceCalcs = pendingPriceCalculationJpa.findAll().map { p ->
            BackupPendingPriceCalculation(
                id = p.id,
                product_id = p.product.id,
                product_name = p.product.name,
                current_sale_price = p.currentSalePrice,
                calculated_price = p.calculatedPrice,
                final_price = p.finalPrice,
                cmv = p.cmv,
                created_at = p.createdAt,
            )
        }
        val productMetricsList = productMetricsJpa.findAll().map { m ->
            BackupProductMetrics(
                id = m.id,
                product_id = m.product.id,
                product_name = m.product.name,
                period_start = m.periodStart,
                period_end = m.periodEnd,
                period_type = m.periodType.name,
                preco_venda = m.precoVenda,
                cmv_aj = m.cmvAj,
                margem_lucro_percent = m.margemLucroPercent,
                lucro_unitario = m.lucroUnitario,
                quantidade_vendida = m.quantidadeVendida,
                numero_pedidos = m.numeroPedidos,
                dias_com_estoque = m.diasComEstoque,
                estoque_medio = m.estoqueMedio,
                faturamento_total = m.faturamentoTotal,
                margem_total = m.margemTotal,
                created_at = m.createdAt,
                updated_at = m.updatedAt,
            )
        }
        val productClassificationsList = productClassificationJpa.findAll().map { c ->
            BackupProductClassification(
                id = c.id,
                product_id = c.product.id,
                product_name = c.product.name,
                period_start = c.periodStart,
                period_end = c.periodEnd,
                period_type = c.periodType.name,
                abc_faturamento = c.abcFaturamento?.name,
                abc_margem = c.abcMargem?.name,
                xyz_giro = c.xyzGiro?.name,
                created_at = c.createdAt,
                updated_at = c.updatedAt,
            )
        }

        val clients = clientJpa.findAll().map { client ->
            BackupClient(
                id = client.id,
                name = client.name,
                phone = client.phone,
                age = client.age,
                location = client.location,
                canal = client.canal,
                interests = client.interests,
            )
        }

        val orders = customerOrderJpa.findAll().map { order ->
            BackupOrder(
                id = order.id,
                payment_method = order.paymentMethod,
                client_id = order.client?.id,
                client_phone = order.client?.phone,
                created_at = order.createdAt,
                order_discount_value = order.orderDiscountValue,
                fee_percentage = order.feePercentage,
                fee_value = order.feeValue,
                campaign_id = order.campaign?.id,
            )
        }

        val sales = saleLineJpa.findAll().map { sale ->
            BackupSale(
                id = sale.id,
                order_id = sale.customerOrder.id,
                product_id = sale.product.id,
                product_name = sale.product.name,
                quantity = sale.quantity,
                unit_price = sale.unitPrice,
                discount = sale.discount,
                created_at = sale.createdAt,
                variation_option_id = sale.variationOption?.id,
            )
        }

        val priceConfigs = priceCalculationConfigJpa.findAll().map { config ->
            BackupPriceCalculationConfig(
                id = config.id,
                default_cmv = config.defaultCmv,
                default_loss_percentage = config.defaultLossPercentage,
                default_sales_commission_percentage = config.defaultSalesCommissionPercentage,
                default_card_fee_percentage = config.defaultCardFeePercentage,
                default_tax_percentage = config.defaultTaxPercentage,
                default_packaging_value = config.defaultPackagingValue,
                default_delivery_value = config.defaultDeliveryValue,
                default_average_items_per_order = config.defaultAverageItemsPerOrder,
                default_fixed_expense_percentage = config.defaultFixedExpensePercentage,
                default_profit_margin_percentage = config.defaultProfitMarginPercentage,
            )
        }

        val suppliers = supplierRepository.findAll().map { supplier ->
            BackupSupplier(
                id = supplier.id,
                name = supplier.name,
                website = supplier.website,
                emails = supplier.emails.toList(),
                phones = supplier.phones.toList(),
                freight = supplier.freight,
                min_free_freight = supplier.minFreeFreight,
                min_order_value = supplier.minOrderValue,
                scraper_excluded = supplier.scraperExcluded,
                scraper_success_selectors = supplier.scraperSuccessSelectors,
            )
        }

        val catalogs = catalogJpa.findAll().map { catalog ->
            BackupCatalog(
                id = catalog.id,
                product_id = catalog.product.id,
                product_name = catalog.product.name,
                cost_price = catalog.costPrice,
                sale_price = catalog.salePrice,
                discount_percentage = catalog.discountPercentage,
                final_price = catalog.finalPrice,
                in_catalog = catalog.inCatalog,
                carousel_position = catalog.carouselPosition,
                carousel_description = catalog.carouselDescription,
                carousel_show_price = catalog.carouselShowPrice,
            )
        }
        val cashList = cashJpa.findAll().map { c ->
            BackupCash(
                id = c.id,
                period_type = c.periodType.name,
                start_date = c.startDate,
                end_date = c.endDate,
                initial_investment = c.initialInvestment,
                closing_balance = c.closingBalance,
            )
        }
        val expensesList = expenseJpa.findAll().map { e ->
            BackupExpense(
                id = e.id,
                name = e.name,
                amount = e.amount,
                due_date = e.dueDate,
                paid_at = e.paidAt,
                is_recurring = e.isRecurring,
                recurrence = e.recurrence,
            )
        }
        val inflowsList = cashInflowJpa.findAll().map { i ->
            BackupCashInflow(
                id = i.id,
                amount = i.amount,
                date = i.date,
                description = i.description,
                type = i.type.name,
            )
        }

        val marginStrategies = marginStrategyJpa.findAll().map { ms ->
            BackupMarginStrategy(
                id = ms.id,
                abc_faturamento = ms.abcFaturamento?.name,
                abc_margem = ms.abcMargem?.name,
                xyz_giro = ms.xyzGiro?.name,
                suggested_margin_percentage = ms.suggestedMarginPercentage,
                description = ms.description,
                is_active = ms.isActive,
                created_at = ms.createdAt,
                updated_at = ms.updatedAt,
            )
        }

        val combosWithItems = comboRepository.findAllWithItems()
        val combosBackup = combosWithItems.map { c ->
            BackupCombo(
                id = c.id,
                product_id = c.product.id,
                name = c.name,
                description = c.description,
                image_url = c.imageUrl,
                image_data_base64 = c.imageData?.let { Base64.getEncoder().encodeToString(it) },
                image_media_type = c.imageMediaType,
                sale_price = c.salePrice,
                active = c.active,
                max_available_quantity = c.maxAvailableQuantity,
            )
        }
        val comboItemsBackup = combosWithItems.flatMap { c ->
            c.items.map { i ->
                BackupComboItem(
                    id = i.id,
                    combo_id = c.id,
                    product_id = i.product.id,
                    product_name = i.product.name,
                    quantity = i.quantity,
                )
            }
        }
        val paymentMethodsBackup = paymentMethodConfigJpa.findAll().map { c ->
            BackupPaymentMethodConfig(
                id = c.id,
                payment_method = c.paymentMethod,
                discount_percentage = c.discountPercentage,
                fee_percentage = c.feePercentage,
            )
        }
        val installmentFeesBackup = creditCardInstallmentFeeJpa.findAllByOrderByInstallmentsAsc().map { f ->
            BackupCreditCardInstallmentFee(
                id = f.id,
                installments = f.installments,
                fee_percentage = f.feePercentage ?: BigDecimal.ZERO,
            )
        }
        val catalogPricesByPaymentMethodBackup = catalogPriceByPaymentMethodJpa.findAll().map { p ->
            BackupCatalogPriceByPaymentMethod(
                id = p.id,
                product_id = p.product.id,
                payment_method = p.paymentMethod,
                installments = p.installments,
                base_price = p.basePrice,
                final_price = p.finalPrice,
            )
        }

        val backupData = BackupData(
            version = "1.0",
            timestamp = timestamp,
            tables = BackupTables(
                categories = categories,
                brands = brands,
                products = products,
                purchase_orders = purchaseOrders,
                purchase_order_items = purchaseOrderItems,
                cash = cashList,
                expenses = expensesList,
                cash_inflows = inflowsList,
                stock_movements = stockMovements,
                stock_units = stockUnits,
                stock_unit_expiry = stockUnitExpiries,
                clients = clients,
                orders = orders,
                sales = sales,
                price_calculation_config = priceConfigs,
                suppliers = suppliers,
                catalog = catalogs,
                margin_strategies = marginStrategies,
                combos = combosBackup,
                combo_items = comboItemsBackup,
                pending_price_calculations = pendingPriceCalcs,
                product_metrics = productMetricsList,
                product_classifications = productClassificationsList,
                payment_methods = paymentMethodsBackup,
                payment_method_installment_fees = installmentFeesBackup,
                catalog_prices_by_payment_method = catalogPricesByPaymentMethodBackup,
                catalog_settings = catalogSettingsJpa.findAll().map { s ->
                    BackupCatalogSettings(
                        whatsapp_number = s.whatsappNumber,
                        instagram_handle = s.instagramHandle,
                        address = s.address,
                        logo_url = s.logoUrl,
                        catalog_title = s.catalogTitle,
                        primary_color = s.primaryColor,
                        about_text = s.aboutText,
                        show_prices = s.showPrices,
                        show_cart = s.showCart,
                    )
                },
                marketing_campaigns = marketingCampaignJpa.findAll().map { c ->
                    BackupMarketingCampaign(
                        id = c.id,
                        name = c.name,
                        channel = c.channel,
                        investment = c.investment,
                        start_date = c.startDate,
                        end_date = c.endDate,
                        description = c.description,
                        open_rate = c.openRate,
                        click_rate = c.clickRate,
                        created_at = c.createdAt,
                    )
                },
                sale_returns = saleReturnJpa.findAll().map { r ->
                    BackupSaleReturn(
                        id = r.id,
                        order_id = r.customerOrder.id,
                        returned_at = r.returnedAt,
                        reason = r.reason,
                        created_at = r.createdAt,
                    )
                },
                sale_return_items = saleReturnJpa.findAll().flatMap { r ->
                    r.items.map { item ->
                        BackupSaleReturnItem(
                            id = item.id,
                            sale_return_id = r.id,
                            product_id = item.product.id,
                            quantity = item.quantity,
                            unit_price = item.unitPrice,
                            variation_option = item.variationOption,
                        )
                    }
                },
            ),
        )

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(backupFile, backupData)
        log.info("Backup completed: file saved at ${backupFile.absolutePath}")
        return backupFile.absolutePath
    }
}
