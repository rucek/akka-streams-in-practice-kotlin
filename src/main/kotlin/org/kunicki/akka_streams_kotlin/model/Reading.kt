package org.kunicki.akka_streams_kotlin.model

interface Reading {
    val id: Int
}

data class ValidReading(override val id: Int, val value: Double): Reading

data class InvalidReading(override val id: Int) : Reading