package com.donnclab.kinetiframe

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationIdentityTest {
    @Test
    fun debugApplicationUsesOwnedIdAndFileProvider() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.donnclab.kinetiframe.dev", context.packageName)
        val provider = context.packageManager.resolveContentProvider("${context.packageName}.fileprovider", 0)
        assertNotNull(provider)
        assertEquals(context.packageName, provider?.packageName)
    }
}
