package io.github.numq.haskcore.core.throwable

val Throwable.exception: Exception
    get() = when (this) {
        is Exception -> this

        else -> Exception(this)
    }