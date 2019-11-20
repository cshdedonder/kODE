@file:Suppress("SpellCheckingInspection", "unused", "DuplicatedCode", "LocalVariableName")

package com.cshdedonder.kode.rungekutta

import com.cshdedonder.kode.*
import com.cshdedonder.kode.math.*
import me.tongfei.progressbar.ProgressBar
import kotlin.math.*
import kotlin.system.measureTimeMillis

interface RungeKuttaData {
    val hMin: Double
    val hMax: Double
    val hFac: Double

    val p: Int
    val s: Int

    val aButcher: Matrix
    val bButcher: Vector
    val cButcher: Vector

    val e: Vector
}

abstract class RungeKuttaStepper : Stepper {
    abstract val dt: RungeKuttaData

    protected fun minmax(value: Double) = min(dt.hMax, max(dt.hMin, dt.hFac * value))

    protected fun tol(yn: Vector): Double =
        options.absoluteTolerance ?: 0.0 + (options.relativeTolerance ?: 0.0) * yn.length

    protected fun hCut(h: Double, xn: Double): Double = min(xn + h, options.xStop) - xn
}

abstract class ImplicitRungeKuttaStepper(override val options: ODEOptions, override val dt: RungeKuttaData) :
    RungeKuttaStepper() {

    override fun integrate(): ODEOutput {
        val solution: MutableMap<Double, Vector> = HashMap()
        var successes = 0
        var failures = 0
        //val pb = ProgressBar("Integrating", (options.xStop*1000).toLong())
        val elapsedTime: Long = measureTimeMillis {
            var xn: Double = options.xStart
            var yn: Vector = options.startValues
            var h: Double = options.hInit
            solution[xn] = yn
            //pb.stepTo((xn*1000).toLong())
            while (xn != options.xStop) {
                h = hCut(h, xn)
                val (yn1: Vector, yi: Vector) = step(h, xn, yn)
                val h2: Double = hCut(hNext(h, yn1, yi), xn)
                if (reject(yn1, yi)) {
                    check(h != h2) { "Value of h plateaued at $h" }
                    failures++
                } else {
                    successes++
                    xn += h
                    yn = yn1
                    solution[xn] = yn
                    //pb.stepTo((xn*1000).toLong())
                }
                h = h2
            }
        }
        //pb.stop()
        return ODEOutput(solution, successes, failures, elapsedTime)
    }

    private fun reject(yn: Vector, yi: Vector): Boolean = (yn - yi).length > tol(yn)

    private fun step(h: Double, xn: Double, yn: Vector): Pair<Vector, Vector> {
        val ks: MutableList<Matrix> = ArrayList(dt.p)
        val f: ODEProblem = options.problem
        val c = dt.cButcher
        val b = dt.bButcher
        val A = dt.aButcher
        //ks += dt.e * f(xn + c[0] * h, yn).t
        ks += Matrix.ByRow.fromVectors(dt.s, options.startValues.dimension) { i -> f(xn + h * c[i], yn) }
        val eynt: Matrix = dt.e * yn.t
        val hA: Matrix = h * A
        for (i in 1 until dt.p) {
            ks += (eynt + hA * ks[i - 1]).applyFunByRow { j, v -> f(xn + c[j] * h, v) }
        }
        return Pair(yn + h * ks.last().t * b, yn + h * ks[dt.p - 2].t * b)
    }

    private fun hNext(hOld: Double, yn: Vector, yi: Vector): Double =
        hOld * minmax((tol(yn) / (yn - yi).length).pow(1.0 / dt.p))

}

class IRK4Stepper(options: ODEOptions) : ImplicitRungeKuttaStepper(
    options,
    IRK4Data
) {
    override fun toString(): String =
        "implicit ${dt.s}-stage RK method of order ${dt.p}\nIntegrating from ${options.xStart} to ${options.xStop}, starting value ${options.startValues}"
}

fun stepperIRK4(init: ODEOptions.Builder.() -> Unit): IRK4Stepper =
    IRK4Stepper((ODEOptions.Builder().also(init).build()))

abstract class ExplicitRungeKuttaStepper(override val options: ODEOptions, override val dt: RungeKuttaData) :
    RungeKuttaStepper() {

    override fun integrate(): ODEOutput {
        val solution: MutableMap<Double, Vector> = HashMap()
        var successes = 0
        var failures = 0
        //val pb = ProgressBar("Integrating", (options.xStop*1000).toLong())
        val elapsedTime: Long = measureTimeMillis {
            var xn: Double = options.xStart
            var yn: Vector = options.startValues
            var h: Double = options.hInit
            solution[xn] = yn
            //pb.stepTo((xn*1000).toLong())
            while (xn != options.xStop) {
                h = hCut(h, xn)
                val yn1: Vector = step(h, xn, yn)
                val zn1: Vector = step(2 * h, xn - h, yn)
                val err: Double = error(yn1, zn1)
                val h2: Double = hCut(hNext(h, err, yn), xn)
                if (reject(err, yn)) {
                    check(h != h2) { "Value of h plateaued at $h" }
                    failures++
                } else {
                    successes++
                    xn += h
                    yn = yn1
                    solution[xn] = yn
                    //pb.stepTo((xn*1000).toLong())
                }
                h = h2
            }
        }
        return ODEOutput(solution, successes, failures, elapsedTime)
    }

    private fun step(h: Double, xn: Double, yn: Vector): Vector {
        val ks: MutableList<Vector> = ArrayList(dt.s)
        val f: ODEProblem = options.problem
        val c = dt.cButcher
        val b = dt.bButcher
        val A = dt.aButcher
        ks += f(xn + dt.cButcher[0] * h, yn)
        for (i in 1 until dt.s) {
            ks += f(
                xn + c[i] * h,
                yn + h * (0 until i).map { j -> A[i, j] * ks[j] }.sum()
            )
        }
        return yn + h * (0 until dt.s).map { i -> b[i] * ks[i] }.sum()
    }

    private fun reject(err: Double, yn: Vector): Boolean = err > tol(yn)

    private fun error(yn1: Vector, zn1: Vector): Double = (1.0 / (2.0.pow(dt.p + 1) - 1)) * (yn1 - zn1).l1

    private fun hNext(hOld: Double, err: Double, yn: Vector): Double =
        hOld * minmax((tol(yn) / err).pow(1.0 / dt.p + 1.0))
}

class ERK4Stepper(options: ODEOptions) : ExplicitRungeKuttaStepper(
    options,
    ERK4Data
) {
    override fun toString(): String =
        "explicit ${dt.s}-stage RK method of order ${dt.p}\nIntegrating from ${options.xStart} to ${options.xStop}, starting value ${options.startValues}"
}

fun stepperERK4(init: ODEOptions.Builder.() -> Unit): ERK4Stepper =
    ERK4Stepper(ODEOptions.Builder().also(init).build())

class DIRK3Stepper(options: ODEOptions) : ImplicitRungeKuttaStepper(
    options,
    DIRK3Data
) {
    override fun toString(): String =
        "diagonally implicit ${dt.s}-stage RK method of order ${dt.p}\nIntegrating from ${options.xStart} to ${options.xStop}, starting value ${options.startValues}"
}

fun stepperDIRK3(init: ODEOptions.Builder.() -> Unit): DIRK3Stepper =
    DIRK3Stepper(ODEOptions.Builder().also(init).build())