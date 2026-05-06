package com.example.pesalens

import org.junit.Test
import org.junit.Assert.*

class MainActivityTest {

    @Test
    fun testParseReceivedMessage() {
        val message = "L876543210 Confirmed. You have received Ksh1,500.00 from JOHN DOE on 12/12/2023 at 12:12 PM."
        val result = MpesaParser.parseMessage(message, 123456789L)
        
        assertNotNull(result)
        assertEquals(1500.0, result?.amount ?: 0.0, 0.0)
        assertEquals("Received", result?.type)
        assertEquals("JOHN DOE", result?.name)
    }

    @Test
    fun testParseSentMessage() {
        val message = "L876543211 Confirmed. Ksh2,450.00 sent to JANE SMITH on 13/12/2023 at 10:00 AM."
        val result = MpesaParser.parseMessage(message, 123456790L)
        
        assertNotNull(result)
        assertEquals(2450.0, result?.amount ?: 0.0, 0.0)
        assertEquals("Sent", result?.type)
        assertEquals("JANE SMITH", result?.name)
    }

    @Test
    fun testParseMessageWithCommas() {
        val message = "L876543212 Confirmed. You have received Ksh 12,500.50 from BOSS on 14/12/2023."
        val result = MpesaParser.parseMessage(message, 123456791L)
        
        assertNotNull(result)
        assertEquals(12500.5, result?.amount ?: 0.0, 0.0)
        assertEquals("Received", result?.type)
        assertEquals("BOSS", result?.name)
    }

    @Test
    fun testInvalidMessage() {
        val message = "Hello how are you?"
        val result = MpesaParser.parseMessage(message, 123456792L)
        
        assertNull(result)
    }

    @Test
    fun testParsesAirtelMoneyMessageWithProviderHint() {
        val message = "Confirmed. Ksh500.00 sent to MARY WAMBUI on 02/01/2026 at 9:00 AM. Transaction fee Ksh6.00."
        val result = MpesaParser.parseMessage(message, 123456793L, NetworkProvider.AIRTEL)

        assertNotNull(result)
        assertEquals(NetworkProvider.AIRTEL, result?.provider)
        assertEquals("Sent", result?.type)
        assertEquals(500.0, result?.amount ?: 0.0, 0.0)
        assertEquals(6.0, result?.fee ?: 0.0, 0.0)
    }

    @Test
    fun testParsesBalanceAndFulizaLimit() {
        val message = "Confirmed. Your M-PESA balance is Ksh1,234.50. Fuliza M-PESA limit is Ksh5,000.00."
        val result = MpesaParser.parseMessage(message, 123456794L, NetworkProvider.MPESA)

        assertNotNull(result)
        assertEquals("Balance", result?.type)
        assertEquals(0.0, result?.amount ?: -1.0, 0.0)
        assertEquals(1234.5, result?.balance ?: 0.0, 0.0)
        assertEquals(5000.0, result?.fulizaLimit ?: 0.0, 0.0)
    }
}
