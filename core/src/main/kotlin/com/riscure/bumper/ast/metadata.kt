package com.riscure.bumper.ast

import arrow.core.None
import arrow.core.or
import com.riscure.bumper.serialization.OptionAsNullable
import kotlinx.serialization.Serializable

/**
 * Metadata for the top-level elements.
 */
@Serializable
data class Meta(
    /**
     * The location where this element was parsed.
     */
    val location: OptionAsNullable<SourceRange> = None,
    /**
     * This location reflects {@code #line} directives,
     * as for example outputted by the preprocessor, pointing
     * poking through to the location beneath the {@code #include} directive.
     */
    val presumedLocation: OptionAsNullable<Location> = None,
    val doc: OptionAsNullable<String> = None,
    val fromMain: Boolean = true
) {
    val presumedHeader get() = presumedLocation.filter { it.isHeader() }

    val mostOriginalSource get() =
        presumedLocation
            .map { it.sourceFile }
            .or(location.map { it.file })

    companion object {
        @JvmStatic
        val default = Meta(None, None, None, true)
    }
}