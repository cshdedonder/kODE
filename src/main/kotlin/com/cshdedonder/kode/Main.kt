package com.cshdedonder.kode

import com.cshdedonder.kode.math.Vector
import com.cshdedonder.kode.rungekutta.*
import java.io.Console
import kotlin.math.PI

private fun Console.readDouble(prompt: String, default: Double): Double = with(readLine("$prompt [$default]: ")) {
    if (isEmpty()) default else toDouble()
}

fun main() {
    with(System.console()) {
        println("Welcome to kODE by Cedric De Donder\nPlease specify below the parameters for solving the Van der Pol problem\nDefaults values are in brackets.")
        val mu: Double = readDouble("The value of the problem parameter 'mu'?", 2.0)
        val method: String = readLine("Which method should be used, 'ERK4', 'DIRK3', or 'IRK4'?: ").toUpperCase()
        val hInit: Double = readDouble("Initial value of 'h'?", 1.0E-4)
        val relTol: Double = readDouble("Relative tolerance?", 1E-6)
        val absTol: Double = readDouble("Absolute tolerance?", 1E-6)
        println()

        val methodMap: Map<String, (ODEOptions.Builder.() -> Unit) -> Stepper> = mapOf(
            "ERK4" to { init -> stepperERK4(init) },
            "DIRK3" to { init -> stepperDIRK3(init) },
            "IRK4" to { init -> stepperIRK4(init) }
        )

        val solver: ODESolver = solver {
            stepper = (methodMap.getValue(method)) {
                this.hInit = hInit
                xStart = 0.0
                xStop = 2.0 * PI
                problem = { _, y ->
                    Vector.of(
                        y[1],
                        mu * (1.0 - y[0] * y[0]) * y[1] - y[0]
                    )
                }
                startValues = Vector.of(
                    0.0,
                    1.0
                )
                relativeTolerance = relTol
                absoluteTolerance = absTol
            }
            title = "Van Der Pol"
            plot = true
            save = false
        }
        solver.integrate().report()
    }
}

