package com.riscure.langs.c.pp.writer

import java.io.StringWriter

/* Something we can write string output to */
fun interface Printer {
    fun print(s: String)

    companion object {
        @JvmStatic
        fun of(jwriter: java.io.Writer) = Printer { jwriter.write(it) }
    }
}

/* The thing that writes to a printer */
fun interface Writer {
    fun write(output: Printer)

    fun write(): String {
        val s = StringWriter()
        write(Printer.of(s))
        return s.toString()
    }
}

/* Sequential composition of writers */
infix fun Writer.andThen(that: Writer): Writer = Writer { w -> write(w); that.write(w) }

val empty: Writer = Writer { }
fun text(s: String): Writer = Writer { it.print(s) }

fun sequence(writers: Iterable<Writer>, separator: Writer = empty): Writer =
    sequence(writers.iterator(), separator)

private fun sequence(writers: Iterator<Writer>, separator: Writer = empty): Writer =
    if (writers.hasNext()) {
        val w = writers.next()

        if (!writers.hasNext()) w
        else (w andThen separator andThen sequence(writers, separator))
    } else empty
