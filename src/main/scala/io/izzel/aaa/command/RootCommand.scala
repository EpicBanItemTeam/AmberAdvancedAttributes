package io.izzel.aaa.command

import com.google.inject.{Inject, Singleton}
import io.izzel.aaa
import io.izzel.aaa.util.EventUtil._
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.item.inventory.equipment.{EquipmentType, EquipmentTypes}
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

import scala.collection.JavaConverters._
import scala.util.continuations.reset

@Singleton
class RootCommand @Inject()(implicit container: PluginContainer, logger: Logger) {

  import org.spongepowered.api.command.args.GenericArguments._

  private val slotMap: Map[String, Option[EquipmentType]] = Map(
    "main-hand" -> Some(EquipmentTypes.MAIN_HAND),
    "off-hand" -> Some(EquipmentTypes.OFF_HAND),
    "head" -> Some(EquipmentTypes.HEADWEAR),
    "chestplate" -> Some(EquipmentTypes.CHESTPLATE),
    "leggings" -> Some(EquipmentTypes.LEGGINGS),
    "boots" -> Some(EquipmentTypes.BOOTS),
    "global" -> None
  )

  private val slotKey: Text = Text.of("slot")

  private val templateKey: Text = Text.of("template")

  reset {
    waitFor[GameInitializationEvent]
    logger.info("Registering commands ...")
    Sponge.getCommandManager.register(container, CommandSpec.builder
      .child(CommandSpec.builder
        .arguments(choices(slotKey, slotMap.asJava), allOf(string(templateKey)))
        .executor(ApplyExecutor).permission(s"${aaa.id}.command.unapply").build(), "apply", "a")
      .child(CommandSpec.builder
        .arguments(choices(slotKey, slotMap.asJava), allOf(string(templateKey)))
        .executor(UnapplyExecutor).permission(s"${aaa.id}.command.unapply").build(), "unapply", "u")
      .executor(FallbackExecutor).build(), "aaa", aaa.id)
    ()
  }

  object ApplyExecutor extends CommandExecutor {
    // TODO: apply templates to a specific item or global slot
    override def execute(src: CommandSource, args: CommandContext): CommandResult = ???
  }

  object UnapplyExecutor extends CommandExecutor {
    // TODO: unapply templates to a specific item or global slot
    override def execute(src: CommandSource, args: CommandContext): CommandResult = ???
  }

  object FallbackExecutor extends CommandExecutor {
    override def execute(src: CommandSource, args: CommandContext): CommandResult = {
      src.sendMessage(Text.of(TextColors.LIGHT_PURPLE, s"${aaa.name} v${container.getVersion.get}"))
      CommandResult.success
    }
  }
}
