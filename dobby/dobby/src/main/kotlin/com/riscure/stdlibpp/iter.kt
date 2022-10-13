package stdlibpp

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right

fun <A,R,E> Iterable<A>.foldM(acc: R, f: (R, A) -> Either<E, R>): Either<E, R> =
    this.iterator().foldM(acc, f)

fun <A,R,E> Iterator<A>.foldM(acc: R, f: (R,A) -> Either<E, R>): Either<E, R> =
    if (this.hasNext()) {
        f(acc, this.next()).flatMap { right -> this.foldM(right, f) }
    } else { acc.right() }
