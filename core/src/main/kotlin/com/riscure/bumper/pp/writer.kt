package com.riscure.bumper.pp

import java.io.StringWriter
import java.io.Writer

/**
 *  The thing that writes prettily.
 *  A [PP] pretty printer can be invoked multiple times.
 */
fun interface PP {
    fun writeTo(output: Writer)

    fun writeTo(): String =
        StringWriter()
            .apply { writeTo(this) }
            .toString()
}

/* Sequential composition of writers */
infix fun PP.andThen(that: PP): PP = PP { w -> writeTo(w); that.writeTo(w) }

val empty: PP = PP { }
fun text(s: String): PP = PP { it.write(s) }

fun sequence(writers: Iterable<PP>, separator: PP = empty): PP =
    sequence(writers.iterator(), separator)

private fun sequence(writers: Iterator<PP>, separator: PP = empty) = PP { w ->
    for (writer in writers) {
        writer.writeTo(w)
        if (writers.hasNext())
            separator.writeTo(w)
    }
}
