package com.cshdedonder.kode.math

import kotlin.math.sqrt

class Matrix(val nRows: Int, val nCols: Int, init: (Int, Int) -> Double) {

    constructor(dimension: Dimension, init: (Int, Int) -> Double) : this(dimension.nRows, dimension.nCols, init)

    private val elements: Array<DoubleArray> = Array(nRows) { i ->
        DoubleArray(nCols) { j ->
            init(i, j)
        }
    }

    val dimensions: Dimension = Dimension(nRows, nCols)

    operator fun get(i: Int, j: Int): Double = elements[i][j]

    operator fun plus(other: Matrix): Matrix = applyBinaryOp(this, other) { a, b -> a + b }

    operator fun minus(other: Matrix): Matrix = applyBinaryOp(this, other) { a, b -> a - b }

    operator fun times(other: Matrix): Matrix {
        require(nCols == other.nRows) { "Multiplication dimension mismatch: $dimensions and ${other.dimensions}" }
        return Matrix(nRows, other.nCols) { i, j -> (0 until nCols).sumByDouble { k -> this[i, k] * other[k, j] } }
    }

    private fun transpose(): Matrix = Matrix(dimensions.transpose()) { i, j -> get(j, i) }

    val t: Matrix
        get() = transpose()

    fun applyFunByRow(f: (Vector) -> Vector): Matrix {
        val vs: MutableList<Vector> = ArrayList(nRows)
        for (i in 0 until nRows) {
            vs += f(Vector(nCols) { j -> get(i, j) })
        }
        return Matrix(dimensions) { i, j -> vs[i][j] }
    }

    override fun toString(): String = elements.joinToString(separator = " \n ", prefix = "[", postfix = "]") {
        it.joinToString(
            separator = ", ",
            prefix = "[",
            postfix = "]"
        ) { x -> "%e".format(x) }
    }

    override fun hashCode(): Int = elements.contentDeepHashCode()

    override fun equals(other: Any?): Boolean =
        if (other is Matrix) elements.contentDeepEquals(other.elements) else false

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    companion object {

        fun constant(nRows: Int, nCols: Int, value: Number): Matrix = constant(nRows, nCols, value.toDouble())

        fun constant(nRows: Int, nCols: Int, value: Double): Matrix = Matrix(nRows, nCols) { _, _ -> value }

        fun zeros(nRows: Int, nCols: Int) = constant(nRows, nCols, 0.0)

        fun ones(nRows: Int, nCols: Int) = constant(nRows, nCols, 1.0)

        fun eye(dimension: Int) = Matrix(dimension, dimension) { i, j -> if (i == j) 1.0 else 0.0 }

        fun of(nRows: Int, nCols: Int, byRow: Boolean = true, vararg elements: Double): Matrix = if (byRow) {
            Matrix(nRows, nCols) { i, j -> elements[i * nCols + j] }
        } else {
            Matrix(nRows, nCols) { i, j -> elements[j * nRows + i] }
        }

        fun of(nRows: Int, nCols: Int, byRow: Boolean = true, vararg elements: Number): Matrix = if (byRow) {
            Matrix(nRows, nCols) { i, j -> elements[i * nCols + j].toDouble() }
        } else {
            Matrix(nRows, nCols) { i, j -> elements[j * nRows + i].toDouble() }
        }

        fun of(byRow: Boolean = true, vararg elements: Number): Matrix {
            val s = sqrt(elements.size.toDouble()).toInt()
            return if (byRow) {
                Matrix(s, s) { i, j -> elements[i * s + j].toDouble() }
            } else {
                Matrix(s, s) { i, j -> elements[j * s + i].toDouble() }
            }
        }

        private inline fun applyBinaryOp(m1: Matrix, m2: Matrix, crossinline op: (Double, Double) -> Double): Matrix {
            require(m1.dimensions == m2.dimensions) { "Dimension mismatch: ${m1.dimensions} and ${m2.dimensions}" }
            return Matrix(m1.dimensions) { i, j ->
                op(m1[i, j], m2[i, j])
            }
        }
    }

    data class Dimension(val nRows: Int, val nCols: Int) {
        override fun toString(): String = "($nRows x $nCols)"

        fun transpose(): Dimension = Dimension(nCols, nRows)
    }
}

fun Vector.transpose(): Matrix = Matrix(1, dimension) { _, j -> get(j) }

val Vector.t: Matrix
    get() = transpose()

fun Vector.asMatrix(): Matrix = Matrix(dimension, 1) { i, _ -> get(i)}

operator fun Matrix.times(v: Vector): Vector {
    require(nCols == v.dimension) { "Multiplication dimension mismatch: $dimensions and (${v.dimension})" }
    return Vector(nRows) { i -> (0 until nCols).sumByDouble { j -> get(i, j) * v[j] } }
}

operator fun Vector.times(m: Matrix): Matrix = asMatrix() * m

operator fun Number.times(m: Matrix): Matrix = with(this.toDouble()) { Matrix(m.dimensions) { i, j -> this * m[i,j]} }