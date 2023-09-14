package com.riscure.dobby.clang

import arrow.core.*
import java.nio.file.Path
import kotlin.io.path.extension

data class Include(
    /** Included path; may be relative. */
    val path: Path,

    /** Whether this is a '<..>' include or not */
    val sysHeader: Boolean,

    ) {
    val isHeader get() = path.extension == "h"

    /**
     * The quoted or bracketed include.
     */
    val quoted: String get() =
        if (sysHeader) "<$path>" else "\"$path\""

    fun pretty() = "#include $quoted"

    companion object {
        @JvmStatic fun sys(name: String) = Include(Path.of(name), true)
        @JvmStatic fun sys(relpath: Path) = Include(relpath, true)

        @JvmStatic fun quoted(relpath: Path) = Include(relpath, false)
    }
}

/**
 * Model of the include path as a whole.
 */
data class IncludePath(
    val isystem: Set<IPath.Sys>   = emptySet(),
    val iquote : Set<IPath.Quote> = emptySet()
) {

    /**
     * Create an [IncludePath] that extends with [include].
     */
    operator fun plus(include: IPath) = when (include) {
        is IPath.Quote -> copy(iquote  = iquote + include)
        is IPath.Sys   -> copy(isystem = isystem + include)
    }

    /**
     * Relativize an *absolute* path [that].
     * Finds the best prefix in this include path, returning a pair of that prefix [IPath] and the suffix.
     * The best root is defined as the one that has the shortest suffix.
     * Returns empty Option if the given path is not relative to any of the IPaths.
     */
    fun relativize(that: Path): Option<Include> {
        val norm = that.normalize();
        return (isystem + iquote)
            .minBy { it.path.relativize(norm).toString().length }
            .toOption()
            .flatMap { it.relativize(norm) }
    }

    fun relativize(paths: Collection<Path>): Either<Path, List<Include>> =
        paths
            .map {
                relativize(it).toEither { it }
            }
            .sequence()

    fun show(): String =
        (this.iquote.asSequence() + this.isystem.asSequence())
            .joinToString(separator = "\n") {  ip -> "\t- $ip" }

    companion object {
        fun iquote(vararg paths: Path) = IncludePath(iquote = paths.map { IPath.Quote(it) }.toSet())
        fun isystem(vararg paths: Path) = IncludePath(isystem = paths.map { IPath.Sys(it) }
            .toSet())
    }
}

/**
 * Model of a single include path element.
 * The only two concrete types are IPath.Sys and IPath.Quote.
 */
sealed interface IPath {
    fun relativize(path: Path): Option<Include> =
        if (path.startsWith(path)) {
            Include(this.path.relativize(path), this is Sys).some()
        } else none()

    val path: Path

    /**
     * A so-called system include path.
     * When invoking a compiler, these are set with `-isystem` or `-I`.
     */
    data class Sys(override val path: Path): IPath {
        override fun toString(): String = "-isystem $path"
    }

    /**
     * A so-called quoted include path.
     * When invoking a compiler, these are set with `-iquote`.
     */
    data class Quote(override val path: Path): IPath {
        override fun toString(): String = "-iquote $path"
    }
}