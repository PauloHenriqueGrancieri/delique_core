package com.delique.core.platform.backup.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class BackupData(
    val version: String = "1.0",
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val timestamp: LocalDateTime,
    val tables: BackupTables
)

data class BackupTables(
    val categories: List<BackupCategory> = emptyList(),
    val brands: List<BackupBrand> = emptyList(),
    val products: List<BackupProduct>,
    val purchase_orders: List<BackupPurchaseOrder> = emptyList(),
    val purchase_order_items: List<BackupPurchaseOrderItem> = emptyList(),
    val cash: List<BackupCash> = emptyList(),
    val expenses: List<BackupExpense> = emptyList(),
    val cash_inflows: List<BackupCashInflow> = emptyList(),
    val stock_movements: List<BackupStockMovement>,
    val stock_units: List<BackupStockUnit> = emptyList(),
    val stock_unit_expiry: List<BackupStockUnitExpiry> = emptyList(),
    val clients: List<BackupClient>,
    val orders: List<BackupOrder>,
    val sales: List<BackupSale>,
    val price_calculation_config: List<BackupPriceCalculationConfig>,
    val suppliers: List<BackupSupplier>,
    val catalog: List<BackupCatalog>,
    val margin_strategies: List<BackupMarginStrategy> = emptyList(),
    val combos: List<BackupCombo> = emptyList(),
    val combo_items: List<BackupComboItem> = emptyList(),
    val pending_price_calculations: List<BackupPendingPriceCalculation> = emptyList(),
    val product_metrics: List<BackupProductMetrics> = emptyList(),
    val product_classifications: List<BackupProductClassification> = emptyList(),
    val payment_methods: List<BackupPaymentMethodConfig> = emptyList(),
    val payment_method_installment_fees: List<BackupCreditCardInstallmentFee> = emptyList(),
    val catalog_prices_by_payment_method: List<BackupCatalogPriceByPaymentMethod> = emptyList(),
    val catalog_settings: List<BackupCatalogSettings> = emptyList(),
    val marketing_campaigns: List<BackupMarketingCampaign> = emptyList(),
    val sale_returns: List<BackupSaleReturn> = emptyList(),
    val sale_return_items: List<BackupSaleReturnItem> = emptyList()
)

data class BackupCategory(
    val id: Long,
    val name: String,
    val has_validity: Boolean,
    val display_id: Int? = null
)

data class BackupBrand(
    val id: Long,
    val name: String,
    val display_id: Int? = null
)

data class BackupPurchaseOrder(
    val id: Long,
    val total_freight: java.math.BigDecimal,
    val status: String,
    val supplier_id: Long? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val created_at: java.time.LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val delivered_at: java.time.LocalDateTime? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val cancelled_at: java.time.LocalDateTime? = null
)

data class BackupCash(
    val id: Long,
    val period_type: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val start_date: java.time.LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val end_date: java.time.LocalDate,
    val initial_investment: java.math.BigDecimal,
    val closing_balance: java.math.BigDecimal? = null
)

data class BackupPurchaseOrderItem(
    val id: Long,
    val purchase_order_id: Long,
    val product_id: Long,
    val product_name: String,
    val quantity: Int,
    val unit_cost: java.math.BigDecimal,
    val expires_at: String? = null,
    val received_quantity: Int? = null,
    val variation_option_id: Long? = null
)

data class BackupProductVariationOption(
    val id: Long? = null,
    val name: String,
    val image_url: String? = null,
    val image_data_base64: String? = null,
    val image_media_type: String? = null,
    val sku: String? = null
)

data class BackupProduct(
    val id: Long,
    val name: String,
    val description: String?,
    val sku: String? = null,
    val image_url: String? = null,
    val image_data_base64: String? = null,
    val image_media_type: String? = null,
    val product_suppliers: List<BackupProductSupplier> = emptyList(),
    val variation_type: String? = null,
    val variation_options: List<BackupProductVariationOption> = emptyList(),
    // Legacy fields (for backward compatibility)
    val supplier_url: String? = null,
    val supplier_id: Long? = null,
    val supplier_ids: List<Long> = emptyList(),
    val supplier_urls: List<String> = emptyList(),
    val is_kit: Boolean? = null,
    val cash_price: BigDecimal? = null,
    val installment_price: BigDecimal? = null,
    val category_id: Long? = null,
    val category: String? = null,
    val brand_id: Long? = null,
    val display_id: Int? = null,
    val minimum_stock: Int? = null
)

data class BackupProductSupplier(
    val id: Long? = null,
    val supplier_id: Long,
    val url: String? = null,
    val price: BigDecimal? = null,
    val out_of_stock_at_supplier: Boolean = false
)

data class BackupStockMovement(
    val id: Long,
    val product_id: Long,
    val product_name: String,
    val quantity: Int,
    val type: String,
    val details: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val created_at: LocalDateTime,
    val purchase_price: BigDecimal? = null,
    val expires_at: String? = null,
    val sale_id: Long? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val edited_at: LocalDateTime? = null,
    val edit_reason: String? = null,
    val variation_option_id: Long? = null
)

data class BackupClient(
    val id: Long,
    val name: String,
    val phone: String,
    val age: Int? = null,
    val location: String? = null,
    val canal: String? = null,
    val interests: String? = null
)

data class BackupOrder(
    val id: Long,
    val payment_method: String,
    val client_id: Long?,
    val client_phone: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val created_at: LocalDateTime,
    val order_discount_value: BigDecimal? = null,
    val fee_percentage: BigDecimal? = null,
    val fee_value: BigDecimal? = null,
    val campaign_id: Long? = null
)

data class BackupSale(
    val id: Long,
    val order_id: Long? = null, // Nullable for legacy format
    val product_id: Long,
    val product_name: String,
    val quantity: Int,
    val unit_price: BigDecimal,
    val discount: BigDecimal?,
    // Legacy fields (for backward compatibility)
    val payment_method: String? = null,
    val client_id: Long? = null,
    val client_phone: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val created_at: LocalDateTime,
    val variation_option_id: Long? = null
)

data class BackupPriceCalculationConfig(
    val id: Long,
    // New fields
    val default_cmv: BigDecimal? = null,
    val default_loss_percentage: BigDecimal,
    val default_sales_commission_percentage: BigDecimal? = null,
    val default_card_fee_percentage: BigDecimal? = null,
    val default_tax_percentage: BigDecimal? = null,
    val default_packaging_value: BigDecimal? = null,
    val default_delivery_value: BigDecimal? = null,
    val default_average_items_per_order: BigDecimal? = null,
    val default_fixed_expense_percentage: BigDecimal,
    val default_investment_tax_percentage: BigDecimal? = null, // Mantido para compatibilidade com backups antigos
    val default_profit_margin_percentage: BigDecimal,
    // Legacy fields (for backward compatibility)
    val default_purchase_cost: BigDecimal? = null,
    val default_packaging_cost: BigDecimal? = null,
    val default_delivery_cost: BigDecimal? = null,
    val default_variable_cost_percentage: BigDecimal? = null
)

data class BackupSupplier(
    val id: Long,
    val name: String,
    val website: String?,
    val emails: List<String>,
    val phones: List<String>,
    val freight: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val min_free_freight: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val min_order_value: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val scraper_excluded: Boolean = false,
    val scraper_success_selectors: String? = null
)

data class BackupCatalog(
    val id: Long = 0,
    val product_id: Long = 0,
    val product_name: String = "",
    val cost_price: BigDecimal? = null,
    val sale_price: BigDecimal = BigDecimal.ZERO,
    val discount_percentage: BigDecimal? = null,
    val final_price: BigDecimal = BigDecimal.ZERO,
    val in_catalog: Boolean = true,
    val carousel_position: Int? = null,
    val carousel_description: String? = null,
    val carousel_show_price: Boolean? = true
)

data class BackupExpense(
    val id: Long,
    val name: String,
    val amount: BigDecimal,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val due_date: java.time.LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val paid_at: java.time.LocalDateTime? = null,
    val is_recurring: Boolean,
    val recurrence: String? = null
)

data class BackupCashInflow(
    val id: Long,
    val amount: BigDecimal,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val date: java.time.LocalDate,
    val description: String? = null,
    val type: String
)

data class BackupMarginStrategy(
    val id: Long,
    val abc_faturamento: String? = null,
    val abc_margem: String? = null,
    val xyz_giro: String? = null,
    val suggested_margin_percentage: BigDecimal,
    val description: String? = null,
    val is_active: Boolean = true,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val created_at: LocalDateTime? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val updated_at: LocalDateTime? = null
)

data class BackupCombo(
    val id: Long,
    val product_id: Long,
    val name: String,
    val description: String? = null,
    val image_url: String? = null,
    val image_data_base64: String? = null,
    val image_media_type: String? = null,
    val sale_price: BigDecimal,
    val active: Boolean = true,
    val max_available_quantity: Int? = null
)

data class BackupComboItem(
    val id: Long,
    val combo_id: Long,
    val product_id: Long,
    val product_name: String? = null,
    val quantity: Int
)

data class BackupStockUnit(
    val id: String,
    val stock_movement_id: Long,
    val unit_index: Int
)

data class BackupStockUnitExpiry(
    val stock_movement_id: Long,
    val unit_index: Int,
    val expires_at: String
)

data class BackupPendingPriceCalculation(
    val id: Long,
    val product_id: Long,
    val product_name: String,
    val current_sale_price: BigDecimal,
    val calculated_price: BigDecimal,
    val final_price: BigDecimal? = null,
    val cmv: BigDecimal,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val created_at: LocalDateTime
)

data class BackupProductMetrics(
    val id: Long,
    val product_id: Long,
    val product_name: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val period_start: java.time.LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val period_end: java.time.LocalDate,
    val period_type: String,
    val preco_venda: BigDecimal = BigDecimal.ZERO,
    val cmv_aj: BigDecimal = BigDecimal.ZERO,
    val margem_lucro_percent: BigDecimal = BigDecimal.ZERO,
    val lucro_unitario: BigDecimal = BigDecimal.ZERO,
    val quantidade_vendida: Int = 0,
    val numero_pedidos: Int = 0,
    val dias_com_estoque: Int = 0,
    val estoque_medio: BigDecimal = BigDecimal.ZERO,
    val faturamento_total: BigDecimal = BigDecimal.ZERO,
    val margem_total: BigDecimal = BigDecimal.ZERO,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val created_at: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val updated_at: LocalDateTime
)

data class BackupProductClassification(
    val id: Long,
    val product_id: Long,
    val product_name: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val period_start: java.time.LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val period_end: java.time.LocalDate,
    val period_type: String,
    val abc_faturamento: String? = null,
    val abc_margem: String? = null,
    val xyz_giro: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val created_at: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val updated_at: LocalDateTime
)

data class BackupPaymentMethodConfig(
    val id: Long? = null,
    val payment_method: String,
    val discount_percentage: BigDecimal? = null,
    val fee_percentage: BigDecimal? = null
)

data class BackupCreditCardInstallmentFee(
    val id: Long? = null,
    val installments: Int,
    val fee_percentage: BigDecimal
)

data class BackupCatalogPriceByPaymentMethod(
    val id: Long? = null,
    val product_id: Long,
    val payment_method: String,
    val installments: Int? = null,
    val base_price: BigDecimal,
    val final_price: BigDecimal
)

data class BackupCatalogSettings(
    val whatsapp_number: String? = null,
    val instagram_handle: String? = null,
    val address: String? = null,
    val logo_url: String? = null,
    val catalog_title: String? = null,
    val primary_color: String? = null,
    val about_text: String? = null,
    val show_prices: Boolean = true,
    val show_cart: Boolean = true
)

data class BackupMarketingCampaign(
    val id: Long = 0,
    val name: String = "",
    val channel: String = "",
    val investment: BigDecimal = BigDecimal.ZERO,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val start_date: LocalDate = LocalDate.now(),
    @JsonFormat(pattern = "yyyy-MM-dd")
    val end_date: LocalDate = LocalDate.now(),
    val description: String? = null,
    val open_rate: BigDecimal? = null,
    val click_rate: BigDecimal? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val created_at: LocalDateTime = LocalDateTime.now()
)

data class BackupSaleReturn(
    val id: Long,
    val order_id: Long,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val returned_at: LocalDateTime,
    val reason: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val created_at: LocalDateTime = LocalDateTime.now()
)

data class BackupSaleReturnItem(
    val id: Long,
    val sale_return_id: Long,
    val product_id: Long,
    val quantity: Int,
    val unit_price: BigDecimal,
    val variation_option: String? = null
)