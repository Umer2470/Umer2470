package com.example

import com.example.data.api.security.AppActivationManager
import org.junit.Assert.*
import org.junit.Test

class AppActivationManagerTest {

    @Test
    fun testActivationCodeGenerationAndVerification() {
        val installationId = "ABCD-1234-EFGH-5678"
        val code = AppActivationManager.generateActivationCode(installationId)

        assertTrue("Code should start with ACTV-", code.startsWith("ACTV-"))
        val isValid = AppActivationManager.verifyActivationCodeLocally(installationId, code)
        assertTrue("Generated code should be valid for the installation ID", isValid)

        val isInvalid = AppActivationManager.verifyActivationCodeLocally("DIFFERENT-ID-0000", code)
        assertFalse("Code should not be valid for different installation ID", isInvalid)
    }

    @Test
    fun testActivationCodeDeterministic() {
        val installationId = "TEST-INSTALL-ID-999"
        val code1 = AppActivationManager.generateActivationCode(installationId)
        val code2 = AppActivationManager.generateActivationCode(installationId)

        assertEquals("Same installation ID should produce exact deterministic key", code1, code2)
    }
}
