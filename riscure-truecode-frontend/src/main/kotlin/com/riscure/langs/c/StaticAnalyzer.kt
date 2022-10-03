package com.riscure.langs.c

import arrow.typeclasses.Semigroup
import com.riscure.langs.c.parser.UnitState

interface StaticAnalyzer<R>: Semigroup<R> {
    fun analyze(state: UnitState): Result<R>
}