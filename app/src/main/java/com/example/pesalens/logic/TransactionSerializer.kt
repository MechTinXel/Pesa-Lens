package com.example.pesalens.logic

import com.example.pesalens.PesaTransaction
import java.text.SimpleDateFormat
import java.util.*

/**
 * Serializes transactions into a compact, information-dense text block
 * suitable for injection into an LLM system prompt.
 *
 * Strategy:
 *  1. Full raw transaction ledger (truncated if > MAX_RAW)
 *  2. Pre-computed summaries so the model doesn't have to calculate everything
 *  3. All values in Ksh
 */
object TransactionSerializer {

    private const val MAX_RAW = 1500          // max individual rows to include verbatim
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val mthFmt  = SimpleDateFormat("MMM yyyy",         Locale.getDefault())

    fun serialize(transactions: List<PesaTransaction>): String {
        if (transactions.isEmpty()) return "No transactions available."

        val sorted   = transactions.sortedByDescending { it.date }
        val sample   = sorted.take(MAX_RAW)
        val isTrunc  = transactions.size > MAX_RAW

        return buildString {

            // ── 1. META ──────────────────────────────────────────────────────
            appendLine("=== PESA TRANSACTION DATABASE ===")
            appendLine("Total transactions: ${transactions.size}")
            if (isTrunc) appendLine("(Showing most recent $MAX_RAW; summaries cover all ${transactions.size})")
            appendLine("Data period: ${dateFmt.format(Date(sorted.last().date))} → ${dateFmt.format(Date(sorted.first().date))}")
            appendLine()

            // ── 2. PRE-COMPUTED SUMMARIES ────────────────────────────────────
            appendLine("=== COMPUTED SUMMARIES (all ${transactions.size} transactions) ===")
            appendLine()

            // Overall
            val allSent     = transactions.filter { it.type != "Received" }
            val allReceived = transactions.filter { it.type == "Received" }
            val totalFees   = transactions.sumOf { it.fee }
            val latestBal   = transactions.firstOrNull { it.balance != null }?.balance
            val fulizaLim   = transactions.firstOrNull { it.fulizaLimit != null }?.fulizaLimit
            val loans       = transactions.filter { it.isLoan || it.type == "Debt/Loan" }

            appendLine("[OVERALL]")
            appendLine("  total_sent=Ksh ${"%.2f".format(allSent.sumOf { it.amount })} (${allSent.size} txns)")
            appendLine("  total_received=Ksh ${"%.2f".format(allReceived.sumOf { it.amount })} (${allReceived.size} txns)")
            appendLine("  net_flow=Ksh ${"%.2f".format(allReceived.sumOf { it.amount } - allSent.sumOf { it.amount })}")
            appendLine("  total_fees=Ksh ${"%.2f".format(totalFees)}")
            latestBal?.let   { appendLine("  last_known_balance=Ksh ${"%.2f".format(it)} (at ${dateFmt.format(Date(transactions.first { t -> t.balance != null }.date))})") }
            fulizaLim?.let   { appendLine("  fuliza_limit=Ksh ${"%.2f".format(it)}") }
            if (loans.isNotEmpty())
                appendLine("  fuliza_total=Ksh ${"%.2f".format(loans.sumOf { it.amount })} (${loans.size} events)")
            appendLine()

            // By transaction type
            appendLine("[BY TYPE]")
            transactions.groupBy { it.type }
                .map { (type, txns) -> Triple(type, txns.sumOf { it.amount }, txns.size) }
                .sortedByDescending { it.second }
                .forEach { (type, amt, cnt) ->
                    appendLine("  $type: Ksh ${"%.2f".format(amt)} ($cnt txns, avg Ksh ${"%.2f".format(amt / cnt)})")
                }
            appendLine()

            // Airtime specifically
            val airtime = transactions.filter { t ->
                val m = t.rawMessage.lowercase()
                m.contains("you have purchased airtime") || t.name.lowercase().contains("airtime") ||
                (t.name.lowercase().contains("safaricom") && m.contains("airtime"))
            }
            val dataBundles = transactions.filter { t ->
                val m = t.rawMessage.lowercase()
                m.contains("data bundle") || m.contains("internet bundle") || m.contains("mb for") || m.contains("gb for")
            }
            if (airtime.isNotEmpty())
                appendLine("[AIRTIME] total=Ksh ${"%.2f".format(airtime.sumOf { it.amount })} count=${airtime.size} avg=Ksh ${"%.2f".format(airtime.sumOf { it.amount } / airtime.size)}")
            if (dataBundles.isNotEmpty())
                appendLine("[DATA BUNDLES] total=Ksh ${"%.2f".format(dataBundles.sumOf { it.amount })} count=${dataBundles.size}")
            appendLine()

            // Monthly breakdown (all months)
            appendLine("[MONTHLY BREAKDOWN - all months]")
            transactions.groupBy { mthFmt.format(Date(it.date)) }
                .entries
                .sortedByDescending { mthFmt.parse(it.key)?.time ?: 0L }
                .forEach { (month, txns) ->
                    val mSent = txns.filter { it.type != "Received" }.sumOf { it.amount }
                    val mRecv = txns.filter { it.type == "Received" }.sumOf { it.amount }
                    val mFees = txns.sumOf { it.fee }
                    appendLine("  $month: sent=Ksh ${"%.2f".format(mSent)} received=Ksh ${"%.2f".format(mRecv)} fees=Ksh ${"%.2f".format(mFees)} count=${txns.size}")
                }
            appendLine()

            // Top 30 contacts sent to
            appendLine("[TOP RECIPIENTS (sent to)]")
            allSent.groupBy { it.name }
                .map { (name, txns) -> Triple(name, txns.sumOf { it.amount }, txns.size) }
                .sortedByDescending { it.second }
                .take(30)
                .forEach { (name, amt, cnt) ->
                    appendLine("  $name: Ksh ${"%.2f".format(amt)} ($cnt txns)")
                }
            appendLine()

            // Top 20 senders received from
            appendLine("[TOP SENDERS (received from)]")
            allReceived.groupBy { it.name }
                .map { (name, txns) -> Triple(name, txns.sumOf { it.amount }, txns.size) }
                .sortedByDescending { it.second }
                .take(20)
                .forEach { (name, amt, cnt) ->
                    appendLine("  $name: Ksh ${"%.2f".format(amt)} ($cnt txns)")
                }
            appendLine()

            // Fee analysis
            appendLine("[FEE ANALYSIS]")
            val withFees = transactions.filter { it.fee > 0 }
            if (withFees.isNotEmpty()) {
                appendLine("  transactions_with_fees=${withFees.size}")
                appendLine("  avg_fee=Ksh ${"%.2f".format(totalFees / withFees.size)}")
                appendLine("  max_fee=Ksh ${"%.2f".format(withFees.maxOf { it.fee })} (${withFees.maxByOrNull { it.fee }?.name})")
                withFees.groupBy { it.type }
                    .mapValues { it.value.sumOf { t -> t.fee } }
                    .entries.sortedByDescending { it.value }
                    .forEach { (type, fee) -> appendLine("  $type fees: Ksh ${"%.2f".format(fee)}") }
            }
            appendLine()

            // Largest / smallest
            val biggest  = transactions.maxByOrNull { it.amount }
            val smallest = transactions.minByOrNull { it.amount }
            appendLine("[EXTREMES]")
            biggest?.let  { appendLine("  largest: Ksh ${"%.2f".format(it.amount)} — ${it.name} — ${it.type} — ${dateFmt.format(Date(it.date))}") }
            smallest?.let { appendLine("  smallest: Ksh ${"%.2f".format(it.amount)} — ${it.name} — ${it.type} — ${dateFmt.format(Date(it.date))}") }
            appendLine()

            // ── 3. FULL TRANSACTION LEDGER ───────────────────────────────────
            appendLine("=== FULL TRANSACTION LEDGER (most recent first) ===")
            appendLine("format: DATE | PROVIDER | TYPE | NAME | AMOUNT | FEE | BALANCE_AFTER")
            appendLine()

            sample.forEach { t ->
                val bal = if (t.balance != null) "bal=Ksh ${"%.2f".format(t.balance)}" else ""
                val fee = if (t.fee > 0) "fee=Ksh ${"%.2f".format(t.fee)}" else ""
                val loan = if (t.isLoan) "[LOAN]" else ""
                appendLine("${dateFmt.format(Date(t.date))} | ${t.provider.shortName} | ${t.type} | ${t.name} | Ksh ${"%.2f".format(t.amount)} $fee $bal $loan".trimEnd())
            }

            if (isTrunc) {
                appendLine()
                appendLine("... and ${transactions.size - MAX_RAW} older transactions (covered in summaries above)")
            }
        }
    }
}
