package com.riscure

interface Failable {
    fun fail(reason: Throwable)

    companion object {
        /* Mixing object */
        val tantrum = object: Failable {
            override fun fail(reason: Throwable) = throw reason
        }
    }
}