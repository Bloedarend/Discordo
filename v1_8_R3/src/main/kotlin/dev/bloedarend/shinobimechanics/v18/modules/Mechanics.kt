package dev.bloedarend.shinobimechanics.v18.modules

import dev.bloedarend.shinobimechanics.api.modules.IMechanics
import dev.bloedarend.shinobimechanics.v18.entities.WireKunai
import net.minecraft.server.v1_8_R3.EntityHuman
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Player

class Mechanics : IMechanics {
    override fun spawnWireKunai(player: Player) {
        val craftPlayer: CraftPlayer = player as CraftPlayer
        val entityHuman: EntityHuman = craftPlayer.handle as EntityHuman
        val world = entityHuman.world

        val wireKunai = WireKunai(world, entityHuman)
        val location = player.eyeLocation

        wireKunai.setLocation(location.x, location.y, location.z, location.yaw, location.pitch)
        world.addEntity(wireKunai)
    }
}