package com.cshdedonder.kode

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

    fun integrate(): ODEOutput = stepper.integrate().also {
        if (plot) {
            with(Chart(it.solution, title, save)) {
                setSize(1000, 1000)
                setLocationRelativeTo(null)
                defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
                isVisible = true
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

    private val dataset: XYDataset = XYSeriesCollection().apply {
        val nVars: Int = solution.values.first().dimension
        val componentSeries: Array<XYSeries> = Array(nVars) { i -> XYSeries("y$i") }
        solution.forEach { (x, v) ->
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
            ChartUtils.saveChartAsPNG(File("$plotName.png"), chart, 800, 8000)
        }
    }
}

data class ODEOptions(
    val hInit: Double,
    val xStart: Double,
    val xStop: Double,
    val startValues: Vector,
    val problem: ODEProblem, //Autonome notatie
    val relativeTolerance: Double? = null,
    val absoluteTolerance: Double? = null
) {
    class Builder {
        var hInit: Double = 1e-6
        var xStart: Double = 0.0
        var xStop: Double = 1.0
        lateinit var startValues: Vector
        lateinit var problem: ODEProblem //Autonome notatie
        var relativeTolerance: Double? = null
        var absoluteTolerance: Double? = null

        fun build(): ODEOptions =
            ODEOptions(hInit, xStart, xStop, startValues, problem, relativeTolerance, absoluteTolerance)
    }
}

typealias ODEProblem = (Vector) -> Vector //Autonoom
typealias ODESolution = Map<Double, Vector>

data class ODEOutput(
    val hHistory: List<Double>,
    val solution: ODESolution,
    val successes: Int,
    val failures: Int,
    val elapsedTime: Long
) {
    fun report(ps: PrintStream = System.out) = with(ps) {
        println("Elapsed Time: ${elapsedTime}ms")
        println("Number of integration points: ${solution.size}")
        println("Number of successes/failures (ratio): ${successes}/${failures} (${"%.3f".format(successes.toDouble() / failures)})")
        println("Average value of h: ${"%e".format(hHistory.average())}")
    }
}

fun solver(init: ODESolver.Builder.() -> Unit): ODESolver = ODESolver.Builder().also(init).build()