package dev.bloedarend.shinobimechanics.plugin.utils

import dev.bloedarend.shinobimechanics.api.modules.IMechanics

class Modules {

    lateinit var mechanics: IMechanics

    fun setModules(version: String) {
        when (version) {
            "v1_8_R3" -> {
                mechanics = dev.bloedarend.shinobimechanics.v18.modules.Mechanics()
            }
            "v1_12_R1" -> { }
        }
    }

}