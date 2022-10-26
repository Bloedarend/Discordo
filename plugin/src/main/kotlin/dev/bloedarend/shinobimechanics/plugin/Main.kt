package dev.bloedarend.shinobimechanics.plugin

import dev.bloedarend.shinobimechanics.plugin.utils.*
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin

class Main: JavaPlugin() {

    // Create an instance of every util class
    val helpers = Helpers()
    val modules = Modules()
    private val configs = Configs(this)

    lateinit var messages: Messages
    lateinit var events: Events
    lateinit var commands: Commands

    // We use this to check the NMS version and determine if the plugin is compatible.
    private lateinit var version: String
    private val versions: List<String> = listOf("v1_8_R3", "v1_12_R1")

    override fun onLoad() {
        version = helpers.getNMSVersion()
    }

    override fun onEnable() {

        if (versions.contains(version)) {
            configs.loadConfigs(this)

            // Can't register command listener before plugin is enabled and configs need to be registered, otherwise they return null.
            messages = Messages(configs, modules)
            events = Events(configs, helpers, messages, modules)
            commands = Commands(configs, events, messages, this)

            commands.registerCommands()
            events.registerListeners(this)
            modules.setModules(version)
        }

        else {
            val errorMessage = listOf(
                "&8----------------------------< &rPvP Mechanics &8>----------------------------",
                " &rThis version is currently not supported! To request support for ",
                " version &c${version}&r, please open a ticket on &chttps://bloedarend.dev/discord",
                "&8-------------------------------------------------------------------------"
            ).joinToString("\n")

            server.consoleSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "\n\n${errorMessage}\n"))
            return Bukkit.getPluginManager().disablePlugin(this)
        }
    }

}