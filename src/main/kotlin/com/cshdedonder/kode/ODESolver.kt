@file:Suppress("SpellCheckingInspection", "MemberVisibilityCanBePrivate")

package com.cshdedonder.kode

import com.cshdedonder.kode.math.Matrix
import com.cshdedonder.kode.math.Vector
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.ChartUtils
import org.jfree.chart.ui.ApplicationFrame
import org.jfree.data.xy.XYDataset
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.io.File
import java.io.PrintStream
import javax.swing.WindowConstants

class ODESolver(
    private val stepper: Stepper,
    private val title: String = "Plot",
    private val plot: Boolean = true,
    private val save: Boolean = false
) {

    fun integrate(): ODEOutput {
        println("Using $stepper")
        return stepper.integrate().also {
            if (plot) {
                println("Making plot ...")
                with(Chart(it.solution, title, save)) {
                    setSize(1000, 1000)
                    setLocationRelativeTo(null)
                    defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
                    isVisible = true
                }
            }
        }
    }

    class Builder {
        lateinit var stepper: Stepper
        var title: String = "Plot"
        var plot: Boolean = true
        var save: Boolean = false

        fun build(): ODESolver = ODESolver(stepper, title, plot, save)
    }
}

class Chart(solution: ODESolution, plotName: String, save: Boolean) : ApplicationFrame("ODE Plot") {

    /**
     * Sample the map so that the remaing values have size [size]]/[rate].
     */
    private fun <K, V> Map<K, V>.sample(rate: Int): Sequence<Map.Entry<K, V>> =
                   asSequence().mapIndexedNotNull { i, entry -> if (i % rate == 0) entry else null }


    private val dataset: XYDataset = XYSeriesCollection().apply {
        val nVars: Int = solution.values.first().dimension
        val componentSeries: Array<XYSeries> = Array(nVars) { i -> XYSeries("y$i") }
        (if(solution.size > 100000) solution.sample(1000) else solution.asSequence()).forEach { (x, v) ->
            for (i in 0 until v.dimension) {
                componentSeries[i].add(x, v[i])
            }
        }
        componentSeries.forEach { addSeries(it) }
    }

    init {
        val chart = ChartFactory.createXYLineChart(plotName, "x", "y", dataset)
        contentPane = ChartPanel(chart)
        if (save) {
            var n = plotName.replace(" ","")
            println("Saving plot as $n.png")
            ChartUtils.saveChartAsPNG(File("$n.png"), chart, 1000, 800)
        }
    }
}

data class ODEOptions(
    val hInit: Double,
    val xStart: Double,
    val xStop: Double,
    val startValues: Vector,
    val problem: ODEProblem,
    val relativeTolerance: Double? = null,
    val absoluteTolerance: Double? = null,
    val jacobian: ODEJacobian? = null
) {
    class Builder {
        var hInit: Double = 1e-6
        var xStart: Double = 0.0
        var xStop: Double = 1.0
        lateinit var startValues: Vector
        lateinit var problem: ODEProblem
        var relativeTolerance: Double? = null
        var absoluteTolerance: Double? = null
        var jacobian: ODEJacobian? = null

        fun build(): ODEOptions {
            check(relativeTolerance != null || absoluteTolerance != null) { "Specify at least one tolerance." }
            return ODEOptions(
                hInit,
                xStart,
                xStop,
                startValues,
                problem,
                relativeTolerance,
                absoluteTolerance,
                jacobian
            )
        }
    }
}

typealias ODEProblem = (Double, Vector) -> Vector
typealias ODESolution = Map<Double, Vector>
typealias ODEJacobian = (Double, Vector) -> Matrix

data class ODEOutput(
    val solution: ODESolution,
    val successes: Int,
    val failures: Int,
    val elapsedTime: Long
) {
    fun report(ps: PrintStream = System.out) = with(ps) {
        println("Elapsed Time: ${elapsedTime}ms")
        println("Number of integration points: ${solution.size}")
        println("Number of failures/successes (ratio): ${failures}/${successes} (${"%.3f".format(failures.toDouble() / successes)})")
        println("Average h used: ${solution.asSequence().map {it.key}.zipWithNext().map{ it.second - it.first}.average()}")
    }
}

fun solver(init: ODESolver.Builder.() -> Unit): ODESolver = ODESolver.Builder().also(init).build()