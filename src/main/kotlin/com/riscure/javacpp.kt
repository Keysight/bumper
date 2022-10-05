package com.riscure

import org.bytedeco.javacpp.*
import arrow.core.*

fun IntPointer.getOption() = if (isNull) None else get().some()
