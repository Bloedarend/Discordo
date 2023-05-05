package dev.bloedarend.discordo.plugin.utils

import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.commands.*
import org.bukkit.plugin.Plugin
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.BukkitCommandHandler
import revxrsal.commands.bukkit.exception.InvalidPlayerException
import revxrsal.commands.bukkit.exception.SenderNotConsoleException
import revxrsal.commands.bukkit.exception.SenderNotPlayerException
import revxrsal.commands.exception.CommandInvocationException
import revxrsal.commands.exception.InvalidBooleanException
import revxrsal.commands.exception.InvalidCommandException
import revxrsal.commands.exception.InvalidNumberException
import revxrsal.commands.exception.InvalidSubcommandException
import revxrsal.commands.exception.MissingArgumentException
import revxrsal.commands.exception.NoPermissionException
import revxrsal.commands.exception.NoSubcommandSpecifiedException
import revxrsal.commands.exception.NumberNotInRangeException
import revxrsal.commands.exception.TooManyArgumentsException

class CommandUtil private constructor() {

    companion object {
        private lateinit var bukkitCommandHandler: BukkitCommandHandler

        fun createCommandHandler(plugin: Plugin) {
            bukkitCommandHandler = BukkitCommandHandler.create(plugin)
        }

        fun registerCommands(plugin: Main) {
            bukkitCommandHandler.failOnTooManyArguments()

            bukkitCommandHandler.register(Broadcast(plugin))
            bukkitCommandHandler.register(Default(plugin))
            bukkitCommandHandler.register(Help())
            bukkitCommandHandler.register(Invite(plugin))
            bukkitCommandHandler.register(Reload(plugin))

            // Set the exception messages to the ones defined in the language.yml
            registerExceptions()
        }

        fun unregisterCommands() {
            bukkitCommandHandler.unregisterAllCommands()
        }

        private fun registerExceptions() {
            bukkitCommandHandler.registerExceptionHandler(CommandInvocationException::class.java) { actor, _ ->
                MessageUtil.sendMessage("exceptions.error", (actor as BukkitCommandActor).sender)
            }

            bukkitCommandHandler.registerExceptionHandler(InvalidBooleanException::class.java) { actor, exception ->
                MessageUtil.sendMessage("errors.invalid-boolean", (actor as BukkitCommandActor).sender,
                    Pair("%input%", exception.input)
                )
            }

            bukkitCommandHandler.registerExceptionHandler(InvalidCommandException::class.java) { actor, exception ->
                MessageUtil.sendMessage("errors.invalid-command", (actor as BukkitCommandActor).sender,
                    Pair("%input%", exception.input)
                )
            }

            bukkitCommandHandler.registerExceptionHandler(InvalidNumberException::class.java) { actor, exception ->
                MessageUtil.sendMessage("errors.invalid-number", (actor as BukkitCommandActor).sender,
                    Pair("%input%", exception.input)
                )
            }

            bukkitCommandHandler.registerExceptionHandler(InvalidPlayerException::class.java) { actor, exception ->
                MessageUtil.sendMessage("errors.invalid-player", (actor as BukkitCommandActor).sender,
                    Pair("%input%", exception.input)
                )
            }

            bukkitCommandHandler.registerExceptionHandler(InvalidSubcommandException::class.java) { actor, exception ->
                MessageUtil.sendMessage("errors.invalid-command", (actor as BukkitCommandActor).sender,
                    Pair("%input%", exception.input)
                )
            }

            bukkitCommandHandler.registerExceptionHandler(NoPermissionException::class.java) { actor, _ ->
                MessageUtil.sendMessage("exceptions.no-permission", (actor as BukkitCommandActor).sender)
            }

            bukkitCommandHandler.registerExceptionHandler(NoSubcommandSpecifiedException::class.java) { actor, _ ->
                MessageUtil.sendMessage("exceptions.no-subcommand", (actor as BukkitCommandActor).sender)
            }

            bukkitCommandHandler.registerExceptionHandler(NumberNotInRangeException::class.java) { actor, exception ->
                MessageUtil.sendMessage("errors.number-not-in-range", (actor as BukkitCommandActor).sender,
                    Pair("%input%", exception.input.toString()),
                    Pair("%min", exception.maximum.toString()),
                    Pair("max", exception.maximum.toString())
                )
            }

            bukkitCommandHandler.registerExceptionHandler(MissingArgumentException::class.java) { actor, exception ->
                MessageUtil.sendMessage("errors.missing-argument", (actor as BukkitCommandActor).sender,
                    Pair("%parameter%", exception.parameter.name)
                )
            }

            bukkitCommandHandler.registerExceptionHandler(SenderNotConsoleException::class.java) { actor, exception ->
                MessageUtil.sendMessage("exceptions.sender-not-console", (actor as BukkitCommandActor).sender)
            }

            bukkitCommandHandler.registerExceptionHandler(SenderNotPlayerException::class.java) { actor, exception ->
                MessageUtil.sendMessage("exceptions.sender-not-player", (actor as BukkitCommandActor).sender)
            }

            bukkitCommandHandler.registerExceptionHandler(TooManyArgumentsException::class.java) { actor, exception ->
                MessageUtil.sendMessage("errors.incorrect-usage", (actor as BukkitCommandActor).sender,
                    Pair("%usage%", exception.command.usage)
                )
            }
        }
    }

}