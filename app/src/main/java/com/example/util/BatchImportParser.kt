package com.example.util

import org.json.JSONArray
import org.json.JSONObject

data class BatchProductImportItem(
    val name: String,
    val category: String = "General",
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val stockQuantity: Double = 0.0,
    val unit: String = "Pcs",
    val barcode: String = "",
    val minStockLevel: Double = 5.0,
    val isExisting: Boolean = false,
    val existingProductId: Long = 0L,
    val currentStock: Double = 0.0
)

data class BatchImportResult(
    val addedCount: Int,
    val updatedCount: Int,
    val errors: List<String>
)

object BatchImportParser {

    val DEMO_SHIPMENT_CSV = """
Product Name,Category,Purchase Price,Sale Price,Stock Quantity,Unit,Barcode
PVC Pipe 1/2 inch 10ft,Plumbing,120,160,50,Pcs,8900101
Brass Ball Valve 1/2 inch,Plumbing,280,350,30,Pcs,8900102
Emulsion Paint White 4L,Paint & Finishes,850,1100,12,Ltr,8900103
Steel Screws 1.5 inch (100pk),Hardware & Fittings,140,200,25,Pack,8900104
Red Bricks Standard Grade 100pk,Building Materials,1200,1500,60,Pack,8900105
Hammer Heavy Duty 16oz,Tools,350,480,10,Pcs,8900106
PVC Solvent Cement 100ml,Adhesives & Chemicals,75,110,40,Pcs,8900107
Wall Plugs Plastic 6mm (100pk),Hardware & Fittings,45,80,50,Pack,8900108
    """.trimIndent()

    val SAMPLE_CSV_HEADER_INFO = """
Format expected (CSV columns):
Product Name, Category, Purchase Price, Sale Price, Quantity, Unit, Barcode

Example:
CPVC Pipe 1 inch, Plumbing, 220, 280, 40, Pcs, 8901011
    """.trimIndent()

    fun parseInputText(text: String): List<BatchProductImportItem> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        return if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            parseJson(trimmed)
        } else {
            parseCsv(trimmed)
        }
    }

    private fun parseCsv(csvText: String): List<BatchProductImportItem> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val items = mutableListOf<BatchProductImportItem>()
        var hasHeader = false
        var colIndices = mapOf<String, Int>()

        // Check if first row is header
        val firstLineParts = splitLine(lines.first())
        val lowerFirstParts = firstLineParts.map { it.lowercase() }
        if (lowerFirstParts.any { it.contains("name") || it.contains("item") || it.contains("category") || it.contains("price") || it.contains("qty") || it.contains("quantity") }) {
            hasHeader = true
            val indexMap = mutableMapOf<String, Int>()
            lowerFirstParts.forEachIndexed { index, name ->
                when {
                    name.contains("name") || name.contains("item") -> indexMap["name"] = index
                    name.contains("category") || name.contains("cat") -> indexMap["category"] = index
                    name.contains("purchase") || name.contains("buy") || name.contains("cost") -> indexMap["purchasePrice"] = index
                    name.contains("sale") || name.contains("sell") || name.contains("price") -> indexMap["salePrice"] = index
                    name.contains("qty") || name.contains("quantity") || name.contains("stock") -> indexMap["stockQuantity"] = index
                    name.contains("unit") -> indexMap["unit"] = index
                    name.contains("barcode") || name.contains("code") -> indexMap["barcode"] = index
                    name.contains("min") -> indexMap["minStockLevel"] = index
                }
            }
            colIndices = indexMap
        }

        val startIndex = if (hasHeader) 1 else 0

        for (i in startIndex until lines.size) {
            val parts = splitLine(lines[i])
            if (parts.isEmpty()) continue

            if (hasHeader && colIndices.containsKey("name")) {
                val nameIdx = colIndices["name"] ?: 0
                val name = parts.getOrNull(nameIdx) ?: continue
                if (name.isBlank()) continue

                val category = colIndices["category"]?.let { parts.getOrNull(it) } ?: "General"
                val purchasePrice = colIndices["purchasePrice"]?.let { parts.getOrNull(it)?.toDoubleOrNull() } ?: 0.0
                val salePrice = colIndices["salePrice"]?.let { parts.getOrNull(it)?.toDoubleOrNull() } ?: 0.0
                val stockQuantity = colIndices["stockQuantity"]?.let { parts.getOrNull(it)?.toDoubleOrNull() } ?: 0.0
                val unit = colIndices["unit"]?.let { parts.getOrNull(it) } ?: "Pcs"
                val barcode = colIndices["barcode"]?.let { parts.getOrNull(it) } ?: ""
                val minStock = colIndices["minStockLevel"]?.let { parts.getOrNull(it)?.toDoubleOrNull() } ?: 5.0

                items.add(
                    BatchProductImportItem(
                        name = name.trim(),
                        category = category.ifBlank { "General" }.trim(),
                        purchasePrice = purchasePrice,
                        salePrice = salePrice,
                        stockQuantity = stockQuantity,
                        unit = unit.ifBlank { "Pcs" }.trim(),
                        barcode = barcode.trim(),
                        minStockLevel = minStock
                    )
                )
            } else {
                // Positional CSV: Name, Category, PurchasePrice, SalePrice, Quantity, Unit, Barcode
                val name = parts.getOrNull(0) ?: continue
                if (name.isBlank()) continue

                val category = parts.getOrNull(1) ?: "General"
                val purchasePrice = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                val salePrice = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                val stockQuantity = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                val unit = parts.getOrNull(5) ?: "Pcs"
                val barcode = parts.getOrNull(6) ?: ""

                items.add(
                    BatchProductImportItem(
                        name = name.trim(),
                        category = category.ifBlank { "General" }.trim(),
                        purchasePrice = purchasePrice,
                        salePrice = salePrice,
                        stockQuantity = stockQuantity,
                        unit = unit.ifBlank { "Pcs" }.trim(),
                        barcode = barcode.trim()
                    )
                )
            }
        }

        return items
    }

    private fun parseJson(jsonText: String): List<BatchProductImportItem> {
        val items = mutableListOf<BatchProductImportItem>()
        try {
            val jsonArray = if (jsonText.startsWith("[")) {
                JSONArray(jsonText)
            } else {
                val obj = JSONObject(jsonText)
                obj.optJSONArray("products") ?: obj.optJSONArray("items") ?: JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val name = obj.optString("name", obj.optString("productName", "")).trim()
                if (name.isBlank()) continue

                val category = obj.optString("category", "General").trim()
                val purchasePrice = obj.optDouble("purchasePrice", obj.optDouble("cost", 0.0))
                val salePrice = obj.optDouble("salePrice", obj.optDouble("price", 0.0))
                val stockQuantity = obj.optDouble("stockQuantity", obj.optDouble("quantity", obj.optDouble("qty", 0.0)))
                val unit = obj.optString("unit", "Pcs").trim()
                val barcode = obj.optString("barcode", "").trim()
                val minStock = obj.optDouble("minStockLevel", 5.0)

                items.add(
                    BatchProductImportItem(
                        name = name,
                        category = if (category.isBlank()) "General" else category,
                        purchasePrice = if (purchasePrice.isNaN()) 0.0 else purchasePrice,
                        salePrice = if (salePrice.isNaN()) 0.0 else salePrice,
                        stockQuantity = if (stockQuantity.isNaN()) 0.0 else stockQuantity,
                        unit = if (unit.isBlank()) "Pcs" else unit,
                        barcode = barcode,
                        minStockLevel = if (minStock.isNaN()) 5.0 else minStock
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    private fun splitLine(line: String): List<String> {
        // Delimiters: comma, tab, pipe, or semicolon
        val delimiter = when {
            line.contains("\t") -> "\t"
            line.contains("|") -> "|"
            line.contains(";") -> ";"
            else -> ","
        }
        return line.split(delimiter).map { it.trim().removeSurrounding("\"") }
    }
}
