package com.cshdedonder.kode

import com.cshdedonder.kode.math.*
import com.cshdedonder.kode.rungekutta.*
import kotlin.math.PI

fun main() {
    val mu = 0.0
    val solver: ODESolver = solver {
        stepper = stepperERK4 {
        //stepper = stepperDIRK3 {
        //stepper = stepperIRK4 {
            hInit = 1e-4
            xStart = 0.0
            xStop = 2.0 * PI
            problem = { _, y ->
                Vector.of(
                    y[1],
                    mu * (1.0 - y[0] * y[0]) * y[1] - y[0]
                )
            }
            jacobian = {_, y ->
                Matrix.ByRow.of(
                    0.0, 1.0,
                    2.0*mu*y[0]*y[1]-1.0, mu*(1.0-y[0]*y[0])
                )
            }
            startValues = Vector.of(
                0.0,
                1.0
            )
            relativeTolerance = 1e-8
            absoluteTolerance = 1e-8
        }
        title = "Van Der Pol (DIRK)"
        plot = true
        save = false
    }
    val solution = solver.integrate()
    solution.report()
}
