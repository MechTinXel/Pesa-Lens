package com.example.pesalens

/**
 * Unified SMS parser for all Kenyan mobile money networks:
 *  - M-Pesa (Safaricom)
 *  - Airtel Money
 *  - T-Kash (Telkom)
 *  - Faiba M-Pesa
 *
 * Returns null if the message is not a transaction SMS.
 */
object SmsParser {

    // ── Paybill / till number → human-readable merchant map ──────────────────
    private val MERCHANT_MAP = mapOf(
        "700700"  to "KPLC Postpaid",
        "888888"  to "KPLC Prepaid",
        "400222"  to "Equity Bank",
        "522522"  to "KCB Bank",
        "303030"  to "Absa Bank",
        "222222"  to "Family Bank",
        "982100"  to "Zuku",
        "510800"  to "DStv",
        "823000"  to "Safaricom Home",
        "247247"  to "Equity Paybill",
        "200999"  to "Nairobi Water",
        "880100"  to "NHIF",
        "572572"  to "NSSF",
        "969000"  to "Kenya Power (ERC)",
        "345345"  to "Safaricom Postpay",
        "777777"  to "Airtel Paybill",
        "444400"  to "I&M Bank",
        "600100"  to "Cooperative Bank",
        "770770"  to "NCBA Bank",
        "100200"  to "Standard Chartered",
        "606060"  to "Prime Bank",
        "102102"  to "HF Group",
        "501501"  to "Stanbic Bank",
        "174174"  to "Diamond Trust Bank"
    )

    fun parseMessage(message: String, timestamp: Long): PesaTransaction? {
        return when {
            NetworkProvider.MPESA.matches(message)  -> parseMpesa(message, timestamp)
            NetworkProvider.AIRTEL.matches(message) -> parseAirtel(message, timestamp)
            NetworkProvider.TELKOM.matches(message) -> parseTelkom(message, timestamp)
            NetworkProvider.FAIBA.matches(message)  -> parseFaiba(message, timestamp)
            else -> null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  M-PESA PARSER
    // ─────────────────────────────────────────────────────────────────────────
    private fun parseMpesa(message: String, timestamp: Long): PesaTransaction? {

        // ── Amount (multiple patterns) ────────────────────────────────────────
        val amount = extractAmount(message) ?: return null

        // ── Transaction reference ─────────────────────────────────────────────
        val ref = Regex("""^([A-Z0-9]{10,12})\s""").find(message.trim())?.groupValues?.get(1)

        // ── Fee ──────────────────────────────────────────────────────────────
        val fee = Regex("""(?i)Transaction\s+cost[,:]?\s*Ksh\s*([\d,]+\.?\d*)""")
            .find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: Regex("""(?i)charge[sd]?\s+Ksh\s*([\d,]+\.?\d*)""")
                .find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: 0.0

        // ── Balance ──────────────────────────────────────────────────────────
        // Multiple possible formats Safaricom uses over the years
        val balance = listOf(
            Regex("""(?i)New M-PESA balance is Ksh\s*([\d,]+\.?\d*)"""),
            Regex("""(?i)M-PESA balance[:\s]+Ksh\s*([\d,]+\.?\d*)"""),
            Regex("""(?i)balance\s+is\s+Ksh\s*([\d,]+\.?\d*)"""),
            Regex("""(?i)balance:\s*Ksh\s*([\d,]+\.?\d*)"""),
            Regex("""(?i)Ksh\s*([\d,]+\.?\d*)\s*\.?\s*M-PESA balance"""),
            Regex("""(?i)your\s+balance\s+(?:is\s+)?Ksh\s*([\d,]+\.?\d*)""")
        ).firstNotNullOfOrNull { regex ->
            regex.find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        }

        // ── Fuliza limit ──────────────────────────────────────────────────────
        val fulizaLimit = listOf(
            Regex("""(?i)Fuliza\s+M-PESA\s+limit\s+is\s+Ksh\s*([\d,]+\.?\d*)"""),
            Regex("""(?i)Fuliza\s+limit[:\s]+Ksh\s*([\d,]+\.?\d*)"""),
            Regex("""(?i)you\s+can\s+overdraw\s+up\s+to\s+Ksh\s*([\d,]+\.?\d*)"""),
            Regex("""(?i)overdraft\s+limit[:\s]+Ksh\s*([\d,]+\.?\d*)""")
        ).firstNotNullOfOrNull { regex ->
            regex.find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        }

        // ── Classify type ─────────────────────────────────────────────────────
        val isFuliza   = message.contains("Fuliza", ignoreCase = true)
        val isMshwari  = message.contains("M-Shwari", ignoreCase = true) || message.contains("KCB M-PESA", ignoreCase = true)
        val isReceived = message.contains("received", ignoreCase = true) && !message.contains("not received", ignoreCase = true)
        val isSentTo   = message.contains("sent to", ignoreCase = true)
        val isPaidTo   = message.contains("paid to", ignoreCase = true) || message.contains("payment to", ignoreCase = true)
        val isPaybill  = message.contains("paybill", ignoreCase = true) || message.contains("pay bill", ignoreCase = true)
        val isWithdraw = message.contains("Withdraw", ignoreCase = true) || message.contains("withdrawn", ignoreCase = true)
        val isDeposit  = message.contains("deposit", ignoreCase = true) || message.contains("deposited", ignoreCase = true)
        val isAirtime  = message.contains("airtime", ignoreCase = true) && !isReceived
        val isReversed = message.contains("reversed", ignoreCase = true)

        val type = when {
            isReversed          -> "Reversed"
            isFuliza || isMshwari -> "Debt/Loan"
            isWithdraw          -> "Withdraw"
            isDeposit           -> "Deposit"
            isAirtime           -> "Airtime"
            isPaybill           -> "Paybill"
            isPaidTo && !isPaybill -> "Buy Goods"
            isSentTo            -> "Sent"
            isReceived          -> "Received"
            else                -> "Other"
        }

        // ── Extract name ──────────────────────────────────────────────────────
        val name = when {
            isReceived -> extractNameAfter(message, listOf("received", "from")) ?: "Unknown Sender"
            isWithdraw -> extractNameAfter(message, listOf("from agent", "at", "agent")) ?: "M-Pesa Agent"
            isDeposit  -> extractNameAfter(message, listOf("to", "for")) ?: "Cash Deposit"
            isPaidTo || isPaybill || isSentTo -> {
                val rawName = extractNameAfter(message, listOf("to", "paid to", "sent to")) ?: "Unknown"
                MERCHANT_MAP[rawName.trim()] ?: rawName
            }
            isFuliza   -> "Fuliza M-Pesa"
            isMshwari  -> "M-Shwari / KCB"
            isAirtime  -> "Safaricom Airtime"
            else        -> "M-Pesa Transaction"
        }

        return PesaTransaction(
            amount      = amount,
            type        = type,
            name        = name.trim().take(50),
            date        = timestamp,
            rawMessage  = message,
            fee         = fee,
            isLoan      = isFuliza || isMshwari,
            balance     = balance,
            fulizaLimit = fulizaLimit,
            provider    = NetworkProvider.MPESA,
            reference   = ref
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AIRTEL MONEY PARSER
    // ─────────────────────────────────────────────────────────────────────────
    private fun parseAirtel(message: String, timestamp: Long): PesaTransaction? {
        val amount = extractAmount(message) ?: return null

        val fee = Regex("""(?i)(?:charge|fee)[d]?[:\s]+(?:Ksh|KES)?\s*([\d,]+\.?\d*)""")
            .find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        val balance = listOf(
            Regex("""(?i)(?:new\s+)?(?:airtel\s+money\s+)?balance[:\s]+(?:Ksh|KES)?\s*([\d,]+\.?\d*)"""),
            Regex("""(?i)balance\s+is\s+(?:Ksh|KES)?\s*([\d,]+\.?\d*)"""),
            Regex("""(?i)(?:Ksh|KES)\s*([\d,]+\.?\d*)\s*(?:is\s+your|your)\s+balance""")
        ).firstNotNullOfOrNull { it.find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() }

        val isReceived = message.contains("received", ignoreCase = true) ||
                         message.contains("credited", ignoreCase = true)
        val isSent     = message.contains("sent", ignoreCase = true) ||
                         message.contains("transferred", ignoreCase = true) ||
                         message.contains("paid", ignoreCase = true)
        val isWithdraw = message.contains("withdraw", ignoreCase = true)
        val isDeposit  = message.contains("deposit", ignoreCase = true)

        val type = when {
            isWithdraw -> "Withdraw"
            isDeposit  -> "Deposit"
            isReceived -> "Received"
            isSent     -> "Sent"
            else       -> "Airtel Money"
        }

        val name = when {
            isReceived -> extractNameAfter(message, listOf("from", "by")) ?: "Airtel Sender"
            isSent     -> extractNameAfter(message, listOf("to")) ?: "Airtel Recipient"
            else       -> "Airtel Money"
        }

        return PesaTransaction(
            amount    = amount,
            type      = type,
            name      = name.trim().take(50),
            date      = timestamp,
            rawMessage = message,
            fee       = fee,
            isLoan    = false,
            balance   = balance,
            provider  = NetworkProvider.AIRTEL
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  T-KASH (TELKOM) PARSER
    // ─────────────────────────────────────────────────────────────────────────
    private fun parseTelkom(message: String, timestamp: Long): PesaTransaction? {
        val amount = extractAmount(message) ?: return null

        val fee = Regex("""(?i)(?:charge|fee|cost)[d]?[:\s]+(?:Ksh|KES)?\s*([\d,]+\.?\d*)""")
            .find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        val balance = listOf(
            Regex("""(?i)t-kash\s+balance[:\s]+(?:Ksh|KES)?\s*([\d,]+\.?\d*)"""),
            Regex("""(?i)balance[:\s]+(?:Ksh|KES)?\s*([\d,]+\.?\d*)""")
        ).firstNotNullOfOrNull { it.find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() }

        val isReceived = message.contains("received", ignoreCase = true) || message.contains("credited", ignoreCase = true)
        val isSent     = message.contains("sent", ignoreCase = true) || message.contains("paid", ignoreCase = true)

        val type = when {
            isReceived -> "Received"
            isSent     -> "Sent"
            else       -> "T-Kash"
        }

        val name = when {
            isReceived -> extractNameAfter(message, listOf("from")) ?: "T-Kash Sender"
            isSent     -> extractNameAfter(message, listOf("to")) ?: "T-Kash Recipient"
            else       -> "T-Kash"
        }

        return PesaTransaction(
            amount    = amount,
            type      = type,
            name      = name.trim().take(50),
            date      = timestamp,
            rawMessage = message,
            fee       = fee,
            balance   = balance,
            provider  = NetworkProvider.TELKOM
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FAIBA PARSER
    // ─────────────────────────────────────────────────────────────────────────
    private fun parseFaiba(message: String, timestamp: Long): PesaTransaction? {
        val amount = extractAmount(message) ?: return null

        val balance = Regex("""(?i)balance[:\s]+(?:Ksh|KES)?\s*([\d,]+\.?\d*)""")
            .find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        val isReceived = message.contains("received", ignoreCase = true)
        val isSent     = message.contains("sent", ignoreCase = true) || message.contains("paid", ignoreCase = true)

        val type = when {
            isReceived -> "Received"
            isSent     -> "Sent"
            else       -> "Faiba"
        }

        val name = when {
            isReceived -> extractNameAfter(message, listOf("from")) ?: "Faiba Sender"
            isSent     -> extractNameAfter(message, listOf("to")) ?: "Faiba Recipient"
            else       -> "Faiba"
        }

        return PesaTransaction(
            amount    = amount,
            type      = type,
            name      = name.trim().take(50),
            date      = timestamp,
            rawMessage = message,
            fee       = 0.0,
            balance   = balance,
            provider  = NetworkProvider.FAIBA
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SHARED HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Extracts the first currency amount from various Kenyan SMS formats */
    private fun extractAmount(message: String): Double? {
        // Try each pattern in order of specificity
        val patterns = listOf(
            // "Ksh1,234.00" or "Ksh 1,234.00"
            Regex("""(?i)Ksh\.?\s*([\d,]+\.?\d{0,2})"""),
            // "KES 1,234" or "KES1234"
            Regex("""(?i)KES\.?\s*([\d,]+\.?\d{0,2})"""),
            // "1,234.00" standalone (fallback)
            Regex("""(?<![.\d])([\d]{2,}(?:,\d{3})*(?:\.\d{1,2})?)(?!\d)""")
        )
        for (pattern in patterns) {
            val value = pattern.find(message)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull()
            if (value != null && value > 0) return value
        }
        return null
    }

    /**
     * Extracts a person/merchant name that appears after one of the given keywords.
     * Stops at sentence boundaries, numbers, or common SMS punctuation.
     */
    private fun extractNameAfter(message: String, keywords: List<String>): String? {
        for (keyword in keywords) {
            // Build regex: keyword followed by optional whitespace then capture a name
            val pattern = Regex(
                """(?i)${Regex.escape(keyword)}\s+([A-Z][A-Z0-9 .&'\-]{1,40}?)(?:\s+(?:on|for|at|via|Ksh|KES|\d|New|your|confirmed)|\.|,|$)"""
            )
            val match = pattern.find(message)
            if (match != null) {
                val raw = match.groupValues[1].trim()
                if (raw.isNotBlank() && raw.length >= 2) {
                    return MERCHANT_MAP[raw] ?: raw
                }
            }
        }
        return null
    }

    /** Detect which provider sent this SMS (for filtering in MainActivity) */
    fun detectProvider(message: String): NetworkProvider? =
        NetworkProvider.values().firstOrNull { it.matches(message) }
}

// Keep MpesaParser as an alias for backward compatibility
@Deprecated("Use SmsParser instead", ReplaceWith("SmsParser"))
object MpesaParser {
    fun parseMessage(message: String, timestamp: Long) = SmsParser.parseMessage(message, timestamp)
}
