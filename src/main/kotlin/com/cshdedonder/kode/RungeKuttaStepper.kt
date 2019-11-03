@file:Suppress("SpellCheckingInspection", "unused")

package com.cshdedonder.kode

import com.cshdedonder.kode.math.Vector
import com.cshdedonder.kode.math.Matrix
import com.cshdedonder.kode.math.t
import com.cshdedonder.kode.math.times
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

interface RungeKuttaData {
    val hMin: Double
    val hMax: Double
    val hFac: Double

    val pInv: Double
    val p: Int
    val s: Int

    val aButcher: Matrix
    val bButcher: Vector
    val cButcher: Vector

    val e: Vector
}

abstract class RungeKuttaStepper(override val options: ODEOptions, private val dt: RungeKuttaData) : Stepper {

    private val tol: (Vector) -> Double =
        { yn -> options.absoluteTolerance ?: 0.0 + (options.relativeTolerance ?: 0.0) * yn.length }

    override fun integrate(): ODEOutput {
        val t: Long = System.currentTimeMillis()
        val solution: MutableMap<Double, Vector> = TreeMap()
        val hHistory: MutableList<Double> = ArrayList()
        var successes = 0
        var failures = 0
        var xn: Double = options.xStart
        var yn: Vector = options.startValues
        var h: Double = options.hInit
        solution[xn] = yn
        while (xn != options.xStop) {
            h = min(xn + h, options.xStop) - xn
            hHistory += h
            val res: Pair<Vector, Vector> = step(h, yn)
            val yn1: Vector = res.first
            val yi: Vector = res.second
            val h2: Double = min(xn + hNext(h, yn1, yi), options.xStop) - xn
            if (reject(yn1, yi)) {
                check(h != h2) { "Value of h plateaued at $h" }
                failures++
            } else {
                successes++
                xn += h
                yn = yn1
                solution[xn] = yn
            }
            h = h2
        }
        return ODEOutput(hHistory, solution, successes, failures, System.currentTimeMillis() - t)
    }

    private fun reject(yn: Vector, yi: Vector): Boolean = (yn - yi).length > tol(yn)

    private fun step(h: Double, yn: Vector): Pair<Vector, Vector> {
        val ks: MutableList<Matrix> = ArrayList(dt.p)
        val f: ODEProblem = options.problem
        ks += dt.e * f(yn).t
        val eynt: Matrix = dt.e * yn.t
        val hA: Matrix = h * dt.aButcher
        for (i in 1 until dt.p) {
            ks += (eynt + hA * ks[i - 1]).applyFunByRow(f)
        }
        return Pair(yn + h * ks.last().t * dt.bButcher, yn + h * ks[dt.p - 2].t * dt.bButcher)
    }

    private fun minmax(value: Double) = min(dt.hMax, max(dt.hMin, dt.hFac * value))

    private fun hNext(hOld: Double, yn: Vector, yi: Vector): Double =
        hOld * minmax((tol(yn) / (yn - yi).length).pow(dt.pInv))

}

object ERK4Data : RungeKuttaData {
    override val hMin: Double = 1.0 / 3.0
    override val hMax: Double = 6.0
    override val hFac: Double = 0.9

    override val pInv: Double = 1.0 / 4.0
    override val p: Int = 4
    override val s: Int = 4

    override val aButcher: Matrix = Matrix.of(true, 0, 0, 0, 0, 0.5, 0, 0, 0, 0, 0.5, 0, 0, 0, 0, 1, 0)
    override val bButcher: Vector = Vector.of(1.0 / 6.0, 1.0 / 3.0, 1.0 / 3.0, 1.0 / 6.0)
    override val cButcher: Vector = Vector.of(0, 0.5, 0.5, 1)

    override val e: Vector = Vector.ones(s)
}

object DIRK3Data : RungeKuttaData {
    override val hMin: Double = 1.0 / 3.0
    override val hMax: Double = 6.0
    override val hFac: Double = 0.9

    override val pInv: Double = 1.0 / 3.0
    override val p: Int = 3
    override val s: Int = 2

    override val aButcher: Matrix =
        Matrix.of(true, (3.0 - sqrt(3.0)) / 6.0, 0, (3.0 + sqrt(3.0)) / 6.0, (3.0 - sqrt(3.0)) / 6.0)
    override val bButcher: Vector = Vector.of(0.5, 0.5)
    override val cButcher: Vector = Vector.of((3.0 - sqrt(3.0)) / 6.0, (3.0 - sqrt(3.0)) / 6.0)
    override val e: Vector = Vector.ones(s)
}

object IRK4Data : RungeKuttaData {
    override val hMin: Double = 1.0 / 3.0
    override val hMax: Double = 6.0
    override val hFac: Double = 0.9

    override val pInv: Double = 1.0 / 3.0
    override val p: Int = 4
    override val s: Int = 2

    override val aButcher: Matrix =
        Matrix.of(true, 0.25, 0.25 - sqrt(3.0) /6.0, 0.25 + sqrt(3.0) /6.0, 0.25)
    override val bButcher: Vector = Vector.of(0.5, 0.5)
    override val cButcher: Vector = Vector.of((0.25 - sqrt(3.0)) / 6.0, (0.25 - sqrt(3.0)) / 6.0)
    override val e: Vector = Vector.ones(s)
}

class ERK4Stepper(options: ODEOptions) : RungeKuttaStepper(options, ERK4Data)

fun ODESolver.Builder.stepperERK4(init: ODEOptions.Builder.() -> Unit): ERK4Stepper =
    ERK4Stepper(ODEOptions.Builder().also(init).build())

class DIRK3Stepper(options: ODEOptions) : RungeKuttaStepper(options, DIRK3Data)

fun ODESolver.Builder.stepperDIRK3(init: ODEOptions.Builder.() -> Unit): DIRK3Stepper =
    DIRK3Stepper(ODEOptions.Builder().also(init).build())

class IRK4Stepper(options: ODEOptions): RungeKuttaStepper(options, IRK4Data)

fun ODESolver.Builder.stepperIRK4(init: ODEOptions.Builder.() -> Unit): IRK4Stepper =
    IRK4Stepper((ODEOptions.Builder().also(init).build()))