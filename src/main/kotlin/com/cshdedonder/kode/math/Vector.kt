package com.cshdedonder.kode.math

import kotlin.math.absoluteValue
import kotlin.math.sqrt

@Suppress("MemberVisibilityCanBePrivate")
class Vector private constructor(private val contents: DoubleArray) {

    companion object {
        fun of(vararg elements: Double): Vector = Vector(elements)

        fun of(vararg elements: Number): Vector = Vector(elements.map { it.toDouble() }.toDoubleArray())

        fun constant(dim: Int, value: Double): Vector = Vector(DoubleArray(dim) { value })

        fun ones(dim: Int) = constant(dim, 1.0)

        fun ones(v: Vector) = ones(v.dimension)

        private inline fun applyBinaryOp(v: Vector, w: Vector, crossinline op: (Double, Double) -> Double): Vector {
            require(v.dimension == w.dimension) { "Dimension mismatch: ${v.dimension} and ${w.dimension}" }
            return Vector(v.dimension) { op(v[it], w[it]) }
        }
    }

    constructor(dimension: Int, init: (Int) -> Double) : this(DoubleArray(dimension, init))

    val dimension: Int
        get() = contents.size

    val length2: Double by lazy { contents.map { x -> x * x }.sum() }

    val length: Double by lazy { sqrt(length2) }

    val l1: Double
        get() = contents.map { x -> x.absoluteValue }.max()!!

    operator fun plus(other: Vector): Vector = applyBinaryOp(this, other) { a, b -> a + b }

    operator fun minus(other: Vector): Vector = applyBinaryOp(this, other) { a, b -> a - b }

    operator fun unaryMinus(): Vector = Vector(dimension) { i -> -get(i) }

    operator fun get(index: Int): Double = contents[index]

    override fun hashCode(): Int = contents.contentHashCode()

    override fun equals(other: Any?): Boolean {
        if (other is Vector) {
            return contents.contentEquals(other.contents)
        }
        return false
    }

    override fun toString(): String {
        return contents.joinToString(separator = ", ", prefix = "[", postfix = "]") { it.toString() }
    }
}

operator fun Number.times(v: Vector): Vector = with(toDouble()) { Vector(v.dimension) { i -> this * v[i] } }

fun List<Vector>.sum(): Vector = reduce { v, w -> v + w }
