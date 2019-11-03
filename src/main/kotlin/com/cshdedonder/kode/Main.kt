package com.cshdedonder.kode

import com.cshdedonder.kode.math.*
import kotlin.math.PI

fun main() {
    val solver: ODESolver = solver {
        // stepper = stepperERK4 {
        // stepper = stepperDIRK3 {
        stepper = stepperIRK4 {
            hInit = 1e-4
            xStart = 0.0
            xStop = 2 * PI
            problem = { v ->
                Vector.of(v[1], -v[0])
            }
            startValues = Vector.of(0.0, 1.0)
            relativeTolerance = 1e-20
            absoluteTolerance = 1e-20
        }
        title = "Vander Pol"
        plot = false
        save = false
    }
    val sol = solver.integrate()
    sol.report()
}
