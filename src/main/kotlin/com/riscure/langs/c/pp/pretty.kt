/**
 * Based on John Hughes' paper "The Design of a Pretty Printing Library", section 7 and 8,
 * and (more concretely) Bernardy's paper "A pretty but not greedy printer".
 */
package com.riscure.langs.c.pp

data class Line(val indent: Int, val content: String) {
    infix fun shiftr(extraIndent: Int) = copy(indent = indent + extraIndent)

    companion object {
        val empty = Line(0, "")
    }
}

interface Layout<T:Layout<T>> {
    fun flush(): T

    fun text(content: String): T
    infix fun horizontal(that: T): T
    infix fun vertial(that: T): T = this.flush() `horizontal` that
}

interface Doc<T:Doc<T>> : Layout<T> {
    infix fun choice(that: T): T
    val fail: T
}

data class TextLayout(val head: Line, val tail: List<Line> = listOf())
    : Layout<TextLayout> {

    val lines: List<Line>  get() = listOf(head) + tail
    val last: Line         get() = tail.takeLast(1).getOrElse(0, { head })
    val prefix: List<Line> get() = lines.dropLast(1)

    private fun plus(those: List<Line>) = this.copy(tail = tail + those)

    override fun text(content: String) = TextLayout(Line(0, content))

    override fun flush() = this.copy(tail = tail + Line.empty)

    override infix fun horizontal(that: TextLayout) =
        TextLayout.of(prefix
               + Line(last.indent, last.content + that.head.content)
               + that.lines.map { it.copy(indent = last.indent + that.head.content.length) })

    companion object {
        fun of(lines: List<Line>): TextLayout {
            assert(lines.size > 0)
            return TextLayout(lines[0], lines.drop(1))
        }

        fun text(content: String) = TextLayout(Line(0, content))

    }
}

/*
data class TextLayouts(val options: Set<TextLayout>): Doc<TextLayouts> {
    override fun choice(that: TextLayouts): TextLayouts =
        TextLayouts(this.options + that.options)

    override val fail: TextLayouts
        get() = TextLayouts(setOf())

    override fun flush(): TextLayouts = TextLayouts(options
        .map { it.flush() }
        .toSet())

    override fun horizontal(that: TextLayouts): TextLayouts {

    }

}
*/

private fun List<Line>.nest(indent: Int) = map { it `shiftr` indent }
