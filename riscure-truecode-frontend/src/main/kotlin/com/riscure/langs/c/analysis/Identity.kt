package com.riscure.langs.c.analysis

import com.riscure.langs.c.Result
import com.riscure.langs.c.ast.TranslationUnit
import com.riscure.langs.c.parser.UnitState

class Identity : StaticAnalysis<TranslationUnit> {
    override fun analyze(state: UnitState): Result<TranslationUnit> =
        state.ast()
}