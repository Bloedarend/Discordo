package dev.bloedarend.discordo.plugin.utils

import dev.bloedarend.discordo.kord.Bot
import dev.bloedarend.discordo.plugin.Main
import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import org.bukkit.plugin.Plugin
import java.io.File

class Configs(private val main: Main) {

    // Make the HashMap 'static'.
    companion object {
        private val configFiles: HashMap<String, YamlDocument> = HashMap()
    }

    fun loadConfigs(plugin: Plugin) {
        loadConfig("config", "config", plugin)
        loadConfig("language", "language", plugin)
        loadConfig("token", "token", plugin)
    }

    private fun loadConfig(location: String, name: String, plugin: Plugin) {
        val directories = location.split("/")
        var file = plugin.dataFolder

        for ((index, directory) in directories.withIndex()) {
            if (index != directories.lastIndex) file = file.toPath().resolve(directory).toFile()
        }

        val config: YamlDocument = YamlDocument.create(
            File(file, "${directories.last()}.yml"),
            plugin.getResource("$name.yml")!!,
            GeneralSettings.DEFAULT,
            LoaderSettings.builder().setAutoUpdate(true).build(),
            DumperSettings.DEFAULT,
            UpdaterSettings.builder().setVersioning(BasicVersioning("file-version")).build()
        )

        configFiles[location] = config
    }

    fun getConfig(name: String) : YamlDocument? {
        return configFiles[name]
    }

    fun reloadConfigs() {
        for ((_, value) in configFiles) {
            value.reload()
        }

        val oldClient = main.bot.client

        // Pass the new instance of the configs to the utils.
        main.messages = Messages(this)
        main.bot = Bot(main as Plugin, this, main.messages)
        main.events = Events(this, main.helpers, main.messages, main.bot, main.images)
        main.commands = Commands(this, main.events, main.messages, main)

        main.startBot(oldClient)
    }
}