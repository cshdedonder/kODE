package com.cshdedonder.kode.rungekutta

import com.cshdedonder.kode.math.Matrix
import com.cshdedonder.kode.math.Vector
import kotlin.math.sqrt

object ERK4Data : RungeKuttaData {
    override val hMin: Double = 1.0 / 3.0
    override val hMax: Double = 6.0
    override val hFac: Double = 0.9

    override val p: Int = 4
    override val s: Int = 4

    override val aButcher: Matrix = Matrix.ByRow.of(
        0, 0, 0, 0,
        0.5, 0, 0,
        0, 0, 0.5, 0,
        0, 0, 0, 1, 0
    )
    override val bButcher: Vector = Vector.of(1.0 / 6.0, 1.0 / 3.0, 1.0 / 3.0, 1.0 / 6.0)
    override val cButcher: Vector = Vector.of(0, 0.5, 0.5, 1)

    override val e: Vector = Vector.ones(s)
}

object DIRK3Data : RungeKuttaData {
    override val hMin: Double = 1.0 / 3.0
    override val hMax: Double = 6.0
    override val hFac: Double = 0.9

    override val p: Int = 3
    override val s: Int = 2

    override val aButcher: Matrix =
        Matrix.ByRow.of(
            (3.0 - sqrt(3.0)) / 6.0, 0,
            (3.0 + sqrt(3.0)) / 6.0, (3.0 - sqrt(3.0)) / 6.0
        )
    override val bButcher: Vector = Vector.of(0.5, 0.5)
    override val cButcher: Vector = Vector.of((3.0 - sqrt(3.0)) / 6.0, (3.0 - sqrt(3.0)) / 6.0)
    override val e: Vector = Vector.ones(s)
}

object IRK4Data : RungeKuttaData {
    override val hMin: Double = 1.0 / 3.0
    override val hMax: Double = 6.0
    override val hFac: Double = 0.9

    override val p: Int = 4
    override val s: Int = 2

    override val aButcher: Matrix =
        Matrix.ByRow.of(
            0.25, 0.25 - sqrt(3.0) / 6.0,
            0.25 + sqrt(3.0) / 6.0, 0.25
        )
    override val bButcher: Vector = Vector.of(0.5, 0.5)
    override val cButcher: Vector = Vector.of((0.25 - sqrt(3.0)) / 6.0, (0.25 - sqrt(3.0)) / 6.0)
    override val e: Vector = Vector.ones(s)
}