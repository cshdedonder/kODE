package com.cshdedonder.kode

interface Stepper {
    val options: ODEOptions
    fun integrate(): ODEOutput
}
