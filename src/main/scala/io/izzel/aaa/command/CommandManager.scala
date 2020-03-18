package io.izzel.aaa.command

import java.util.{UUID, function}

import com.google.inject.{Inject, Singleton}
import io.izzel.aaa
import io.izzel.aaa.api.data.{Template, TemplateSlot, UnreachableSlotException}
import io.izzel.aaa.attribute.AttributeManager
import io.izzel.aaa.config.ConfigManager
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

import scala.util.continuations.reset

@Singleton
class CommandManager @Inject()(implicit container: PluginContainer, config: ConfigManager, manager: AttributeManager, logger: Logger, locale: AmberLocale) {

  implicit class RichCommandBuilder(b: CommandSpec.Builder) {
    def executor(f: (CommandSource, CommandContext) => Unit): CommandSpec.Builder = b.executor(new CommandExecutor {
      override def execute(source: CommandSource, context: CommandContext): CommandResult = {
        f.apply(source, context)
        CommandResult.success
      }
    })
  }

  private val slotKey: Text = Text.of("slot")
  private val templateKey: Text = Text.of("template")
  private val slotArg: function.Supplier[java.util.Collection[String]] with function.Function[String, TemplateSlot] = {
    new function.Supplier[java.util.Collection[String]] with function.Function[String, TemplateSlot] {
      override def get: java.util.Collection[String] = manager.slotMap.keys.map(_.toString).toList.asJava

      override def apply(key: String): TemplateSlot = Template.tryParse(key).asScala.flatMap(manager.slotMap.get).orNull
    }
  }
  private val templateArg: function.Supplier[java.util.Collection[String]] with function.Function[String, Template] = {
    new function.Supplier[java.util.Collection[String]] with function.Function[String, Template] {
      override def get: java.util.Collection[String] = config.keys.map(_.toString).toSet.asJava

      override def apply(key: String): Template = Template.tryParse(key).asScala.filter(config.get(_).isDefined).orNull
    }
  }

  private def init(src: CommandSource, args: CommandContext): Unit = src match {
    case player: Player => locally {
      val handTypes = Sponge.getRegistry.getAllOf(classOf[HandType]).asScala
      handTypes.find(player.getItemInHand(_).isPresent) match {
        case Some(handType) => locally {
          val item = player.getItemInHand(handType).get
          item.get(classOf[CustomTemplates.Data]).asScala match {
            case None => locally {
              val uuid = new UUID(0, 0)
              val templateData = CustomTemplates.Builder.create()
              val displayName = item.get[Text](Keys.DISPLAY_NAME).asScala
              val lore = item.get[java.util.List[Text]](Keys.ITEM_LORE).asScala.map(_.asScala.toList)

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

  private def drop(src: CommandSource, args: CommandContext): Unit = src match {
    case player: Player => locally {
      var isCallbackExecuted = false
      val handTypes = Sponge.getRegistry.getAllOf(classOf[HandType]).asScala
      handTypes.find(player.getItemInHand(_).isPresent) match {
        case Some(_) => locally {
          val arg = Arg.ref("commands.drop.warning-ok").withCallback(new function.Consumer[CommandSource] {
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

  private def show(src: CommandSource, args: CommandContext): Unit = src match {
    case player: Player => try {
      val slot = args.getOne[TemplateSlot](slotKey).get
      val templates = slot.getTemplates(player).asScala.toSet[Template]
      if (templates.isEmpty) locale.to(src, "commands.show.absent") else {
        locale.to(src, "commands.show.present", Int.box(templates.size))
        val prefix = Text.builder("* ").color(TextColors.BLUE).build()
        for (template <- templates) {
          src.sendMessage(Text.of(prefix, Text.builder(template.toString).color(TextColors.AQUA).build()))
        }
      }
    } catch {
      case _: UnreachableSlotException => locale.to(src, "commands.drop.nonexist")
    }
    case _ => locale.to(src, "commands.drop.nonexist")
  }

  private def apply(src: CommandSource, args: CommandContext): Unit = src match {
    case player: Player => try {
      val slot = args.getOne[TemplateSlot](slotKey).get
      val templates = slot.getTemplates(player).asScala.toSet[Template]
      val templatesToAdd = args.getAll[Template](templateKey).asScala.toSet.diff(templates)
      if (templatesToAdd.isEmpty) locale.to(src, "commands.apply.absent") else {
        slot.setTemplates(player, templates.union(templatesToAdd).toList.asJava)
        locale.to(src, "commands.apply.present", Int.box(templatesToAdd.size))
        val prefix = Text.builder("+ ").color(TextColors.DARK_GREEN).build()
        for (template <- templatesToAdd) {
          src.sendMessage(Text.of(prefix, Text.builder(template.toString).color(TextColors.GREEN).build()))
        }
      }
    } catch {
      case _: UnreachableSlotException => locale.to(src, "commands.drop.nonexist")
    }
    case _ => locale.to(src, "commands.drop.nonexist")
  }

  private def unapply(src: CommandSource, args: CommandContext): Unit = src match {
    case player: Player => try {
      val slot = args.getOne[TemplateSlot](slotKey).get
      val templates = slot.getTemplates(player).asScala.toSet[Template]
      val templatesToAdd = args.getAll[Template](templateKey).asScala.toSet.intersect(templates)
      if (templatesToAdd.isEmpty) locale.to(src, "commands.unapply.absent") else {
        slot.setTemplates(player, templates.diff(templatesToAdd).toList.asJava)
        locale.to(src, "commands.unapply.present", Int.box(templatesToAdd.size))
        val prefix = Text.builder("- ").color(TextColors.DARK_RED).build()
        for (template <- templatesToAdd) {
          src.sendMessage(Text.of(prefix, Text.builder(template.toString).color(TextColors.RED).build()))
        }
      }
    } catch {
      case _: UnreachableSlotException => locale.to(src, "commands.drop.nonexist")
    }
    case _ => locale.to(src, "commands.drop.nonexist")
  }

  private def fallback(src: CommandSource, args: CommandContext): Unit = {
    src.sendMessage(Text.of(TextColors.LIGHT_PURPLE, s"${aaa.name} v${container.getVersion.get}"))
    CommandResult.success
  }

  reset {
    import org.spongepowered.api.command.args.GenericArguments._

    waitFor[GameInitializationEvent]
    logger.info("Registering commands ...")
    Sponge.getCommandManager.register(container, CommandSpec.builder
      .child(CommandSpec.builder
        .arguments(none())
        .executor(init _).permission(s"${aaa.id}.command.init").build(), "init", "i")
      .child(CommandSpec.builder
        .arguments(none())
        .executor(drop _).permission(s"${aaa.id}.command.drop").build(), "drop", "d")
      .child(CommandSpec.builder
        .arguments(choices(slotKey, slotArg, slotArg, true))
        .executor(show _).permission(s"${aaa.id}.command.show").build(), "show", "s")
      .child(CommandSpec.builder
        .arguments(choices(slotKey, slotArg, slotArg, true), allOf(choices(templateKey, templateArg, templateArg, false)))
        .executor(apply _).permission(s"${aaa.id}.command.apply").build(), "apply", "a")
      .child(CommandSpec.builder
        .arguments(choices(slotKey, slotArg, slotArg, true), allOf(choices(templateKey, templateArg, templateArg, false)))
        .executor(unapply _).permission(s"${aaa.id}.command.unapply").build(), "unapply", "u")
      .executor(fallback _).build(), "aaa", aaa.id)
    ()
  }
}
