package vn.hvp.travelvoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import vn.hvp.travelvoice.data.remote.DemoApi

class DemoApiUrlTest {
    @Test fun acceptsLocalAndHttpsAddressesButRejectsCredentialsAndNonHttpSchemes() {
        assertEquals("http://10.0.2.2:8081/", DemoApi.normalizeBaseUrl(" http://10.0.2.2:8081 "))
        assertEquals("https://demo.example/", DemoApi.normalizeBaseUrl("https://demo.example/"))
        for (url in listOf("file:///tmp/data", "javascript:alert(1)", "http://admin:secret@host/", "http://host/?token=secret", "https://demo.example/offline")) {
            assertThrows(Exception::class.java) { DemoApi.normalizeBaseUrl(url) }
        }
    }
}
