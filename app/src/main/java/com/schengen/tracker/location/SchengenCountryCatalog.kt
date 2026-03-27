package com.schengen.tracker.location

data class CountryOption(
    val code: String,
    val name: String
)

object SchengenCountryCatalog {
    val options = listOf(
        CountryOption("AT", "Austria"),
        CountryOption("BE", "Belgium"),
        CountryOption("BG", "Bulgaria"),
        CountryOption("HR", "Croatia"),
        CountryOption("CZ", "Czechia"),
        CountryOption("DK", "Denmark"),
        CountryOption("EE", "Estonia"),
        CountryOption("FI", "Finland"),
        CountryOption("FR", "France"),
        CountryOption("DE", "Germany"),
        CountryOption("GR", "Greece"),
        CountryOption("HU", "Hungary"),
        CountryOption("IS", "Iceland"),
        CountryOption("IT", "Italy"),
        CountryOption("LV", "Latvia"),
        CountryOption("LI", "Liechtenstein"),
        CountryOption("LT", "Lithuania"),
        CountryOption("LU", "Luxembourg"),
        CountryOption("MT", "Malta"),
        CountryOption("NL", "Netherlands"),
        CountryOption("NO", "Norway"),
        CountryOption("PL", "Poland"),
        CountryOption("PT", "Portugal"),
        CountryOption("RO", "Romania"),
        CountryOption("SK", "Slovakia"),
        CountryOption("SI", "Slovenia"),
        CountryOption("ES", "Spain"),
        CountryOption("SE", "Sweden"),
        CountryOption("CH", "Switzerland")
    )

    private val optionsByCode = options.associateBy { it.code }
    private val aliases = mapOf(
        "czech republic" to "CZ"
    )
    private val codeByLookupValue = buildMap {
        options.forEach { option ->
            put(normalizeLookup(option.code), option.code)
            put(normalizeLookup(option.name), option.code)
        }
        aliases.forEach { (alias, code) ->
            put(normalizeLookup(alias), code)
        }
    }

    fun nameForCode(code: String): String = optionsByCode[normalizeCode(code)]?.name ?: code

    fun displayNames(codes: List<String>): List<String> =
        normalizeCodes(codes).map(::nameForCode)

    fun normalizeCode(value: String): String? =
        codeByLookupValue[normalizeLookup(value)]

    fun normalizeCodes(values: Iterable<String>): List<String> =
        values.mapNotNull(::normalizeCode).distinct()

    fun filter(query: String, selectedCodes: Set<String>): List<CountryOption> {
        val normalizedQuery = normalizeLookup(query)
        val filtered = if (normalizedQuery.isBlank()) {
            options
        } else {
            options.filter { option ->
                option.name.lowercase().contains(normalizedQuery) ||
                    option.code.lowercase().contains(normalizedQuery)
            }
        }

        return filtered.sortedWith(
            compareByDescending<CountryOption> { it.code in selectedCodes }
                .thenBy { it.name }
        )
    }

    private fun normalizeLookup(value: String): String =
        value.trim()
            .lowercase()
            .replace(".", "")
            .replace(Regex("\\s+"), " ")
}
