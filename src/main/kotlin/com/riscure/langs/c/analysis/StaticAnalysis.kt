package com.riscure.langs.c.analysis

import arrow.typeclasses.Semigroup
import com.riscure.langs.c.Result
import com.riscure.langs.c.parser.UnitState

interface StaticAnalysis<R> /*: Semigroup<R> */ {
    fun analyze(state: UnitState): Result<R>
}