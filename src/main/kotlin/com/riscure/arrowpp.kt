package com.riscure

import arrow.core.*
import java.util.*

fun <T> Option<T>.toOptional(): Optional<T> = Optional.ofNullable(this.orNull())
