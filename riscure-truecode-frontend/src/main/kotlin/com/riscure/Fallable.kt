package com.riscure

interface Fallable {
    fun fail(reason: Throwable)

    companion object {
        /* Mixing object */
        val tantrum = object: Fallable {
            override fun fail(reason: Throwable) = throw reason
        }
    }
}