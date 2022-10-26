package dev.bloedarend.shinobimechanics.plugin.utils

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.plugin.Plugin
import java.util.regex.Matcher
import java.util.regex.Pattern

class Helpers {

    fun getVersion(plugin: Plugin) : Int {
        // Get the first two values of the minecraft version.
        val version: Matcher = Pattern.compile("\\d+\\.\\d+").matcher(plugin.server.version)
        version.find()

        // Get the second integer of the minecraft version. This will be easier to compare in code later on.
        val comparableVersion: Matcher = Pattern.compile("\\d+").matcher(version.group())
        comparableVersion.find(1)

        return comparableVersion.group().toInt()
    }

    fun getNMSVersion(): String {
        // Get the package name and convert it to a version string formatted as: v0_0_R0
        val version = Bukkit.getServer().javaClass.`package`.name
        return version.substring(version.lastIndexOf('.') + 1)
    }

    fun isInteractable(material: Material, plugin: Plugin) : Boolean {
        val materials = HashSet<String>()

        // Because the bukkit isInteractable method does not include all interactable blocks, I made a scuffed list instead.
        materials.addAll(listOf(
            "ANVIL",
            "ARMOR_STAND",
            "BARREL",
            "BEACON",
            "BED",
            "BELL",
            "BOAT",
            "BREWING_STAND",
            "BUTTON",
            "CRAFTING",
            "COMMAND",
            "COMPARATOR",
            "CHEST",
            "DAYLIGHT_DETECTOR",
            "DISPENSER",
            "DROPPER",
            "DOOR",
            "ENCHANTMENT_TABLE",
            "FENCE_GATE",
            "FURNACE",
            "GRINDSTONE",
            "HOPPER",
            "ITEM_FRAME",
            "LECTERN",
            "LEVER",
            "LOOM",
            "MINECART",
            "NOTE_BLOCK",
            "PRESSURE_PLATE",
            "REPEATER",
            "SIGN",
            "SHULKER_BOX",
            "SMITHING_TABLE",
            "SMOKER",
            "STONECUTTER",
            "WORKBENCH"
        ))

        // In versions 1.16 and above, players can also interact with redstone wire.
        if (getVersion(plugin) > 16) materials.add("REDSTONE_WIRE")

        for (string in materials) {
            if (material.toString().contains(string)) return true
        }

        return false
    }

}