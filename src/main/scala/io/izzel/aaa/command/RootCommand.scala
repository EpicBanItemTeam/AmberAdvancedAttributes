package io.izzel.aaa.command

import java.util.UUID
import java.util.function.Consumer

import com.google.inject.{Inject, Singleton}
import io.izzel.aaa
import io.izzel.aaa.api.data.TemplateSlot
import io.izzel.aaa.attribute.AttributeManager
import io.izzel.aaa.data.CustomTemplates
import io.izzel.aaa.util._
import io.izzel.amber.commons.i18n.AmberLocale
import io.izzel.amber.commons.i18n.args.Arg
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.data.`type`.HandType
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.manipulator.DataManipulator
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

import scala.collection.mutable
import scala.util.continuations.reset

@Singleton
class RootCommand @Inject()(implicit container: PluginContainer, manager: AttributeManager, logger: Logger, locale: AmberLocale) {

  import org.spongepowered.api.command.args.GenericArguments._

  private val slotMap: Map[String, TemplateSlot] = manager.slotMap.map(e => e._1.toString -> e._2)

  private val slotKey: Text = Text.of("slot")

  private val templateKey: Text = Text.of("template")

  reset {
    waitFor[GameInitializationEvent]
    logger.info("Registering commands ...")
    Sponge.getCommandManager.register(container, CommandSpec.builder
      .child(CommandSpec.builder
        .arguments(none())
        .executor(InitExecutor).permission(s"${aaa.id}.command.init").build(), "init", "i")
      .child(CommandSpec.builder
        .arguments(none())
        .executor(DropExecutor).permission(s"${aaa.id}.command.drop").build(), "drop", "d")
      .child(CommandSpec.builder
        .arguments(choices(slotKey, slotMap.asJava), allOf(string(templateKey)))
        .executor(ApplyExecutor).permission(s"${aaa.id}.command.apply").build(), "apply", "a")
      .child(CommandSpec.builder
        .arguments(choices(slotKey, slotMap.asJava), allOf(string(templateKey)))
        .executor(UnapplyExecutor).permission(s"${aaa.id}.command.unapply").build(), "unapply", "u")
      .executor(FallbackExecutor).build(), "aaa", aaa.id)
    ()
  }

  object InitExecutor extends CommandExecutor {
    override def execute(src: CommandSource, args: CommandContext): CommandResult = {
      src match {
        case player: Player => locally {
          val handTypes = Sponge.getRegistry.getAllOf(classOf[HandType]).asScala
          handTypes.find(player.getItemInHand(_).isPresent) match {
            case Some(handType) => locally {
              val item = player.getItemInHand(handType).get
              item.get(classOf[CustomTemplates.Data]).asScala match {
                case None => locally {
                  val uuid = new UUID(0, 0)
                  val templateData = CustomTemplates.Builder.create()
                  val displayName = item.get(Keys.DISPLAY_NAME).asScala
                  val lore = item.get(Keys.ITEM_LORE).asScala.map(_.asScala.toList)

                  templateData.value = CustomTemplates.Value(uuid, CustomTemplates.Backup(displayName, lore), Nil)
                  item.offer(templateData.asInstanceOf[DataManipulator[_, _]])
                  item.remove(Keys.DISPLAY_NAME)
                  item.remove(Keys.ITEM_LORE)

                  src.asInstanceOf[Player].setItemInHand(handType, item)
                  locale.to(src, "commands.init.succeed")
                }
                case Some(_) => locale.to(src, "commands.init.already-exist")
              }
            }
            case None => locale.to(src, "commands.init.nonexist")
          }
        }
        case _ => locale.to(src, "commands.init.nonexist")
      }
      CommandResult.success
    }
  }

  object DropExecutor extends CommandExecutor {
    override def execute(src: CommandSource, args: CommandContext): CommandResult = {
      src match {
        case player: Player => locally {
          var isCallbackExecuted = false
          val handTypes = Sponge.getRegistry.getAllOf(classOf[HandType]).asScala
          handTypes.find(player.getItemInHand(_).isPresent) match {
            case Some(_) => locally {
              val arg = Arg.ref("commands.drop.warning-ok").withCallback(new Consumer[CommandSource] {
                override def accept(t: CommandSource): Unit = if (!isCallbackExecuted) {
                  handTypes.find(player.getItemInHand(_).isPresent) match {
                    case Some(handType) => locally {
                      val item = player.getItemInHand(handType).get
                      item.get(classOf[CustomTemplates.Data]).asScala match {
                        case Some(data) => locally {
                          isCallbackExecuted = true

                          val CustomTemplates.Backup(name, lore) = data.value.backup
                          name.foreach(value => item.offer(Keys.DISPLAY_NAME, value))
                          lore.foreach(value => item.offer(Keys.ITEM_LORE, value.asJava))
                          item.remove(classOf[CustomTemplates.Data])

                          src.asInstanceOf[Player].setItemInHand(handType, item)
                          locale.to(src, "commands.drop.succeed")
                        }
                        case None => locale.to(src, "commands.drop.nonexist")
                      }
                    }
                    case None => locale.to(src, "commands.drop.nonexist")
                  }
                }
              })
              locale.to(src, "commands.drop.warning", arg)
            }
            case None => locale.to(src, "commands.drop.nonexist")
          }
        }
        case _ => locale.to(src, "commands.drop.nonexist")
      }
      CommandResult.success
    }
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
