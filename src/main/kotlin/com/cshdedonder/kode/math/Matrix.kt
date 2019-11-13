package com.cshdedonder.kode.math

import kotlin.math.absoluteValue
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

    inline fun applyFunByRow(f: (Int, Vector) -> Vector): Matrix {
        val vs: MutableList<Vector> = ArrayList(nRows)
        for (i in 0 until nRows) {
            vs += f(i, Vector(nCols) { j -> get(i, j) })
        }
        return Matrix(dimensions) { i, j -> vs[i][j] }
    }

    private fun LUPDecompose(tol: Double = 1e-40): Pair<Matrix, IntArray> {
        check(nRows == nCols) { "Matrix needs to be square for LUP decomposition." }
        val n = nRows
        val p: IntArray = IntArray(n) { i -> i }
        val A = copy()
        println(A)
        for (i in 0 until n) {
            var maxA = 0.0
            var imax = i
            for (k in i until n) {
                if (get(k, i).absoluteValue > maxA) {
                    maxA = get(k, i).absoluteValue
                    imax = k
                }
            }
            check(maxA > tol) { "Matrix is degenerate: maxA = $maxA, A = $A" }
            if (imax != i) {
                val j = p[i]
                p[i] = p[imax]
                p[imax] = j
                val row = A.elements[i]
                A.elements[i] = A.elements[imax]
                A.elements[imax] = row
            }
            for (j in (i + 1) until n) {
                A[j, i] /= A[i, i]
                for (k in (i + 1) until n) {
                    A[j, k] -= A[j, i] * A[i, k]
                }
            }
            println(A)
        }
        return Pair(A, p)
    }

    private operator fun set(i: Int, j: Int, x: Double) {
        elements[i][j] = x
    }

    private fun copy(): Matrix = Matrix(nRows, nCols) { i, j -> get(i, j) }

    fun solve(b: Vector): Vector {
        val (A: Matrix, p: IntArray) = LUPDecompose()
        val n = nRows
        val x = DoubleArray(n)
        for (i in 0 until n) {
            x[i] = b[p[i]]
            for (k in 0 until i) {
                x[i] -= A[i, k] * x[k]
            }
        }
        for (i in (n - 1) downTo 0) {
            for (k in (i + 1) until n) {
                x[i] -= A[i, k] * x[k]
            }
            x[i] /= A[i, i]
        }
        return Vector.of(*x)
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

        private inline fun applyBinaryOp(m1: Matrix, m2: Matrix, crossinline op: (Double, Double) -> Double): Matrix {
            require(m1.dimensions == m2.dimensions) { "Dimension mismatch: ${m1.dimensions} and ${m2.dimensions}" }
            return Matrix(m1.dimensions) { i, j ->
                op(m1[i, j], m2[i, j])
            }
        }
    }

    class ByRow {
        companion object {
            fun of(vararg elements: Number): Matrix {
                val s = sqrt(elements.size.toDouble()).toInt()
                return Matrix(s, s) { i, j -> elements[i * s + j].toDouble() }
            }

            inline fun fromVectors(nRows: Int, nCols: Int, crossinline func: (Int) -> Vector): Matrix =
                    with(Array(nRows) { i -> func(i) }) {
                        return Matrix(nRows, nCols) { i, j -> this[i][j] }
                    }
        }
    }

    class ByColumn {
        companion object {
            fun of(vararg elements: Number): Matrix {
                val s = sqrt(elements.size.toDouble()).toInt()
                return Matrix(s, s) { i, j -> elements[j * s + i].toDouble() }
            }

            inline fun fromVectors(nRows: Int, nCols: Int, crossinline func: (Int) -> Vector): Matrix =
                    Matrix(nRows, nCols) { i, j -> func(j)[i] }
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

fun Vector.asMatrix(): Matrix = Matrix(dimension, 1) { i, _ -> get(i) }

operator fun Matrix.times(v: Vector): Vector {
    require(nCols == v.dimension) { "Multiplication dimension mismatch: $dimensions and (${v.dimension})" }
    return Vector(nRows) { i -> (0 until nCols).sumByDouble { j -> get(i, j) * v[j] } }
}

fun Matrix.row(i: Int): Vector = Vector(nCols) { j -> get(i, j) }

operator fun Vector.times(m: Matrix): Matrix = asMatrix() * m

operator fun Number.times(m: Matrix): Matrix = with(this.toDouble()) { Matrix(m.dimensions) { i, j -> this * m[i, j] } }