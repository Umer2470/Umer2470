package com.example.data.model

enum class UserRole(
    val roleKey: String,
    val displayName: String,
    val level: Int,
    val iconEmoji: String,
    val description: String
) {
    SUPER_ADMIN(
        roleKey = "SUPER_ADMIN",
        displayName = "Super Admin",
        level = 4,
        iconEmoji = "👑",
        description = "Unrestricted master control: Owner center, user privileges, security & licensing"
    ),
    ADMIN(
        roleKey = "ADMIN",
        displayName = "Admin",
        level = 3,
        iconEmoji = "🛡️",
        description = "Store administration: Inventory, purchases, cashier management, reports & settings"
    ),
    SUPERVISOR(
        roleKey = "SUPERVISOR",
        displayName = "Supervisor",
        level = 2,
        iconEmoji = "👨‍💼",
        description = "Store supervisor: POS operations, stock audits, daily closing & attendance logs"
    ),
    CASHIER(
        roleKey = "CASHIER",
        displayName = "Cashier / Staff",
        level = 1,
        iconEmoji = "👤",
        description = "Counter staff: POS sales terminal, invoice reprint & daily punch attendance"
    );

    companion object {
        fun fromString(roleStr: String?): UserRole {
            val clean = roleStr?.trim()?.uppercase()?.replace(" ", "_") ?: return CASHIER
            return when {
                clean.contains("SUPER") -> SUPER_ADMIN
                clean.contains("ADMIN") -> ADMIN
                clean.contains("SUPERVISOR") || clean.contains("MANAGER") -> SUPERVISOR
                clean.contains("CASHIER") || clean.contains("STAFF") || clean.contains("EMPLOYEE") -> CASHIER
                else -> CASHIER
            }
        }

        fun isAllowed(userRole: UserRole, targetRoute: String): Boolean {
            return when (targetRoute) {
                // POS Terminal, Invoices, Attendance, Customers are accessible to Cashier and above
                "pos", "invoice", "attendance", "customers" -> true

                // Inventory, Purchases, Suppliers, Daily Closing are accessible to Supervisor and above
                "inventory", "purchases", "suppliers", "closing" -> userRole.level >= SUPERVISOR.level

                // Reports, Cashier Management, General Settings, Recycle Bin, Setup are accessible to Admin and above
                "reports", "cashier_management", "settings", "recycle_bin", "setup", "logs" -> userRole.level >= ADMIN.level

                // User Management, Store Access Management, Owner Control Center, License Activation, Developer Hub require Super Admin / Admin
                "users", "access_management", "store_management" -> userRole.level >= ADMIN.level
                "owner_control_center", "activation", "developer_hub" -> userRole == SUPER_ADMIN || userRole == ADMIN

                "dashboard" -> true
                else -> true
            }
        }
    }
}
