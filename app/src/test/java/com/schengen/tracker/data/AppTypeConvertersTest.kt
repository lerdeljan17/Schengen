package com.schengen.tracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppTypeConvertersTest {
    private val converters = AppTypeConverters()

    @Test
    fun countryCodes_roundTripThroughJson() {
        val encoded = converters.toCountryCodes(listOf("DE", "FR", "ES"))

        assertEquals(listOf("DE", "FR", "ES"), converters.fromCountryCodes(encoded))
    }

    @Test
    fun decodeCountryCodes_trimsFiltersAndDeduplicatesJsonValues() {
        val decoded = AppTypeConverters.decodeCountryCodes("""[" DE ","","FR","DE"]""")

        assertEquals(listOf("DE", "FR"), decoded)
    }

    @Test
    fun decodeCountryCodes_supportsLegacyDelimitedValues() {
        val decoded = AppTypeConverters.decodeCountryCodes("DE, FR;ES\nPT|DE")

        assertEquals(listOf("DE", "FR", "ES", "PT"), decoded)
    }

    @Test
    fun decodeCountryCodes_returnsEmptyListForBlankValues() {
        assertEquals(emptyList<String>(), converters.fromCountryCodes(null))
        assertEquals(emptyList<String>(), converters.fromCountryCodes(""))
        assertEquals(emptyList<String>(), converters.fromCountryCodes("   "))
    }
}
