package dev.bloedarend.discordo.plugin.utils

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

}