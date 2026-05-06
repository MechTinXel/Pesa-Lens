package com.example.pesalens.logic

import com.example.pesalens.PesaTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

object AIAssistant {

    private const val BASE_URL = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-3-5-sonnet-20240620"
    private val client = OkHttpClient()

    fun askOffline(query: String, transactions: List<PesaTransaction>): String {
        if (transactions.isEmpty()) {
            return "I do not have transactions for the selected filter yet. Try another year or provider."
        }

        val lower = query.lowercase(Locale.ROOT)
        val moneyOut = transactions.filter { it.type != "Received" && it.type != "Balance" && it.amount > 0 }
        val moneyIn = transactions.filter { it.type == "Received" }
        val fees = transactions.sumOf { it.fee }
        val latestBalance = transactions.firstOrNull { it.balance != null }?.balance
        val fulizaLimit = transactions.firstOrNull { it.fulizaLimit != null }?.fulizaLimit
        val loans = transactions.filter { it.isLoan || it.type == "Debt/Loan" }

        return when {
            "fee" in lower || "charge" in lower -> {
                val highestFee = transactions.maxByOrNull { it.fee }
                buildString {
                    appendLine("Fees in this view: Ksh ${fees.money()}.")
                    if (highestFee != null && highestFee.fee > 0) {
                        append("Highest fee was Ksh ${highestFee.fee.money()} on ${highestFee.name}.")
                    }
                }.trim()
            }
            "fuliza" in lower || "loan" in lower || "debt" in lower -> {
                val borrowed = loans.sumOf { it.amount }
                "Fuliza/loan activity totals Ksh ${borrowed.money()} across ${loans.size} events. Latest known Fuliza allowance is ${fulizaLimit?.let { "Ksh ${it.money()}" } ?: "not available from SMS yet"}."
            }
            "balance" in lower -> {
                "Latest known balance is ${latestBalance?.let { "Ksh ${it.money()}" } ?: "not available yet"}. This depends on your provider including balance text in SMS messages."
            }
            "top" in lower || "where" in lower || "spend" in lower || "recipient" in lower -> {
                val top = moneyOut.groupBy { it.name }
                    .map { (name, list) -> Triple(name, list.sumOf { it.amount }, list.size) }
                    .sortedByDescending { it.second }
                    .take(5)

                if (top.isEmpty()) "I do not see spending transactions in this filtered view."
                else top.joinToString(separator = "\n") { (name, amount, count) ->
                    "$name: Ksh ${amount.money()} across $count transactions"
                }
            }
            "income" in lower || "received" in lower -> {
                val total = moneyIn.sumOf { it.amount }
                "Income in this view is Ksh ${total.money()} across ${moneyIn.size} received transactions."
            }
            "month" in lower || "trend" in lower -> monthlyBreakdown(transactions)
            else -> {
                val totalOut = moneyOut.sumOf { it.amount }
                val totalIn = moneyIn.sumOf { it.amount }
                val net = totalIn - totalOut
                "For this filtered view: income is Ksh ${totalIn.money()}, spending is Ksh ${totalOut.money()}, fees are Ksh ${fees.money()}, and net flow is Ksh ${net.money()}."
            }
        }
    }

    suspend fun askStreaming(
        query: String,
        transactions: List<PesaTransaction>,
        history: List<ChatMessage>,
        apiKey: String,
        onToken: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        
        val systemPrompt = TransactionSerializer.serialize(transactions)
        
        val json = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 1024)
            put("system", systemPrompt)
            put("stream", true)
            
            val messagesArray = JSONArray()
            history.forEach { 
                messagesArray.put(JSONObject().apply {
                    put("role", it.role)
                    put("content", it.content)
                })
            }
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", query)
            })
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { 
                    if (!it.isSuccessful) {
                        onError("Error: ${it.code}")
                        return
                    }
                    
                    val source = it.body?.source() ?: return
                    try {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (line.startsWith("data: ")) {
                                val data = line.substring(6)
                                if (data == "[DONE]") break
                                
                                val event = JSONObject(data)
                                val type = event.optString("type")
                                
                                if (type == "content_block_delta") {
                                    val delta = event.getJSONObject("delta")
                                    val text = delta.optString("text")
                                    onToken(text)
                                } else if (type == "message_stop") {
                                    onDone()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        onError(e.message ?: "Streaming error")
                    }
                }
            }
        })
    }

    private fun monthlyBreakdown(transactions: List<PesaTransaction>): String {
        val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return transactions
            .groupBy { formatter.format(Date(it.date)) }
            .entries
            .sortedByDescending { formatter.parse(it.key)?.time ?: 0L }
            .take(6)
            .joinToString(separator = "\n") { (month, items) ->
                val sent = items.filter { it.type != "Received" && it.type != "Balance" }.sumOf { it.amount }
                val received = items.filter { it.type == "Received" }.sumOf { it.amount }
                "$month: sent Ksh ${sent.money()}, received Ksh ${received.money()}, ${items.size} transactions"
            }
    }

    private fun Double.money(): String =
        String.format(Locale.US, "%,.2f", this)
}
