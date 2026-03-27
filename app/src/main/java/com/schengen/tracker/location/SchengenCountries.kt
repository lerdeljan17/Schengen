package com.schengen.tracker.location

object SchengenCountries {
    val isoCodes = SchengenCountryCatalog.options.map { it.code }.toSet()
}
