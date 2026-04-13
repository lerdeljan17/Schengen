package com.schengen.tracker.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchengenCountryCatalogTest {
    @Test
    fun normalizeCode_acceptsCodesNamesAndAliases() {
        assertEquals("DE", SchengenCountryCatalog.normalizeCode(" de "))
        assertEquals("FR", SchengenCountryCatalog.normalizeCode("France"))
        assertEquals("CZ", SchengenCountryCatalog.normalizeCode("Czech Republic"))
        assertNull(SchengenCountryCatalog.normalizeCode("United Kingdom"))
    }

    @Test
    fun normalizeCodes_dropsUnknownsAndDeduplicates() {
        val codes = SchengenCountryCatalog.normalizeCodes(listOf("Germany", "DE", "France", "unknown"))

        assertEquals(listOf("DE", "FR"), codes)
    }

    @Test
    fun displayNames_normalizesBeforeFormatting() {
        val names = SchengenCountryCatalog.displayNames(listOf("de", "France", "Czech Republic"))

        assertEquals(listOf("Germany", "France", "Czechia"), names)
    }

    @Test
    fun nameForCode_returnsOriginalValueForUnknownCode() {
        assertEquals("XX", SchengenCountryCatalog.nameForCode("XX"))
    }

    @Test
    fun filter_matchesByNameOrCodeAndSortsSelectedFirst() {
        val countries = SchengenCountryCatalog.filter("s", selectedCodes = setOf("ES"))
            .map { it.code }

        assertEquals("ES", countries.first())
        assertTrue("SE" in countries)
        assertTrue("SK" in countries)
    }
}
