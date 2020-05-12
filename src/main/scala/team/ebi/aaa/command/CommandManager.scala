package team.ebi.aaa.command

import java.util.{UUID, function}

import com.google.inject.{Inject, Singleton}
import io.izzel.amber.commons.i18n.AmberLocale
import io.izzel.amber.commons.i18n.args.Arg
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.args.{CommandArgs, CommandContext, CommandElement}
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.data.`type`.HandTypes
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.manipulator.DataManipulator
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors
import org.spongepowered.api.util.StartsWithPredicate
import team.ebi.aaa
import team.ebi.aaa.api.data.{Template, UnreachableSlotDataException}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.config.ConfigManager
import team.ebi.aaa.data.CustomTemplates
import team.ebi.aaa.util._

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

  private class TemplateCommandElement(key: Text, values: () => Iterable[Template]) extends CommandElement(key) {
    override def parseValue(src: CommandSource, args: CommandArgs): AnyRef = {
      val next = args.next()
      Template.tryParse(next).asScala match {
        case Some(template) => template
        case None => throw args.createError(Text.of("Invalid template string format: ", next))
      }
    }

    override def complete(src: CommandSource, args: CommandArgs, context: CommandContext): java.util.List[String] = {
      val predicate = new StartsWithPredicate(args.nextIfPresent.orElse(""))
      values().map(_.toString).filter(predicate.test).toList.asJava
    }
  }

  private def useTemplate(src: CommandSource, args: CommandContext): (Seq[Template], Seq[Template] => Unit) = {
    import team.ebi.aaa.api.data.TemplateSlots._
    src match {
      case _ if args.hasAny("global") => (GLOBAL.getTemplates.asScala, (t: Seq[Template]) => GLOBAL.setTemplates(t.asJava))
      case p: Player => (MAIN_HAND.getTemplates(p).asScala, (t: Seq[Template]) => MAIN_HAND.setTemplates(p, t.asJava))
      case _ => (manager.unreachable(ItemStack.empty), (_: Seq[Template]) => ())
    }
  }

  private def init(src: CommandSource, args: CommandContext): Unit = src match {
    case player: Player => player.getItemInHand(HandTypes.MAIN_HAND).asScala match {
      case Some(item) => item.get(classOf[CustomTemplates.Data]).asScala match {
        case None => locally {
          val uuid = new UUID(0, 0)
          val templateData = CustomTemplates.Builder.create()
          val displayName = item.get[Text](Keys.DISPLAY_NAME).asScala
          val lore = item.get[java.util.List[Text]](Keys.ITEM_LORE).asScala.map(_.asScala.toList)

          templateData.value = CustomTemplates.Value(uuid, CustomTemplates.Backup(displayName, lore), Nil)
          item.offer(templateData.asInstanceOf[DataManipulator[_, _]])
          item.remove(Keys.DISPLAY_NAME)
          item.remove(Keys.ITEM_LORE)

          src.asInstanceOf[Player].setItemInHand(HandTypes.MAIN_HAND, item)
          locale.to(src, "commands.init.succeed")
        }
        case Some(_) => locale.to(src, "commands.init.already-exist")
      }
      case None => locale.to(src, "commands.init.nonexist")
    }
    case _ => locale.to(src, "commands.init.nonexist")
  }

  private def drop(src: CommandSource, args: CommandContext): Unit = src match {
    case player: Player => player.getItemInHand(HandTypes.MAIN_HAND).asScala match {
      case Some(item) => item.get(classOf[CustomTemplates.Data]).asScala match {
        case Some(_) => locally {
          var isCallbackExecuted = false
          val arg = Arg.ref("commands.drop.warning-ok").withCallback(new function.Consumer[CommandSource] {
            override def accept(t: CommandSource): Unit = if (!isCallbackExecuted) {
              player.getItemInHand(HandTypes.MAIN_HAND).asScala match {
                case Some(item) => item.get(classOf[CustomTemplates.Data]).asScala match {
                  case Some(data) => locally {
                    isCallbackExecuted = true

                    val CustomTemplates.Backup(name, lore) = data.value.backup
                    name.map(value => item.offer(Keys.DISPLAY_NAME, value)).getOrElse(item.remove(Keys.DISPLAY_NAME))
                    lore.map(value => item.offer(Keys.ITEM_LORE, value.asJava)).getOrElse(item.remove(Keys.ITEM_LORE))
                    item.remove(classOf[CustomTemplates.Data])

                    src.asInstanceOf[Player].setItemInHand(HandTypes.MAIN_HAND, item)
                    locale.to(src, "commands.drop.succeed")
                  }
                  case None => locale.to(src, "commands.drop.nonexist")
                }
                case None => locale.to(src, "commands.drop.nonexist")
              }
            }
          })
          locale.to(src, "commands.drop.warning", arg)
        }
        case None => locale.to(src, "commands.drop.nonexist")
      }
      case None => locale.to(src, "commands.drop.nonexist")
    }
    case _ => locale.to(src, "commands.drop.nonexist")
  }

  private def show(src: CommandSource, args: CommandContext): Unit = try {
    val (templates, _) = useTemplate(src, args)
    templates match {
      case Nil => locale.to(src, "commands.show.absent")
      case _ => locally {
        locale.to(src, "commands.show.present", Int.box(templates.size))
        val prefix = Text.builder("* ").color(TextColors.BLUE).build()
        for (template <- templates) {
          src.sendMessage(Text.of(prefix, Text.builder(template.toString).color(TextColors.AQUA).build()))
        }
      }
    }
  } catch {
    case _: UnreachableSlotDataException => locale.to(src, "commands.drop.nonexist")
  }

  private def apply(src: CommandSource, args: CommandContext): Unit = try {
    val (templates, setTemplates) = useTemplate(src, args)
    args.getAll[Template]("template-to-append").asScala.toList.diff(templates) match {
      case Nil => locale.to(src, "commands.apply.absent")
      case templatesToAppend => locally {
        setTemplates(templates.union(templatesToAppend))
        locale.to(src, "commands.apply.present", Int.box(templatesToAppend.size))
        val prefix = Text.builder("+ ").color(TextColors.DARK_GREEN).build()
        for (template <- templatesToAppend) {
          src.sendMessage(Text.of(prefix, Text.builder(template.toString).color(TextColors.GREEN).build()))
        }
      }
    }
  } catch {
    case _: UnreachableSlotDataException => locale.to(src, "commands.drop.nonexist")
  }

  private def unapply(src: CommandSource, args: CommandContext): Unit = try {
    val (templates, setTemplates) = useTemplate(src, args)
    args.getAll[Template]("template-to-remove").asScala.toList.intersect(templates) match {
      case Nil => locale.to(src, "commands.unapply.absent")
      case templatesToRemove => locally {
        setTemplates(templates.diff(templatesToRemove))
        locale.to(src, "commands.unapply.present", Int.box(templatesToRemove.size))
        val prefix = Text.builder("- ").color(TextColors.DARK_RED).build()
        for (template <- templatesToRemove) {
          src.sendMessage(Text.of(prefix, Text.builder(template.toString).color(TextColors.RED).build()))
        }
      }
    }
  } catch {
    case _: UnreachableSlotDataException => locale.to(src, "commands.drop.nonexist")
  }

  private def fallback(src: CommandSource, args: CommandContext): Unit = {
    src.sendMessage(Text.of(TextColors.LIGHT_PURPLE, s"${aaa.name} v${container.getVersion.get}"))
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
        .arguments(flags().flag("-global").buildWith(none()))
        .executor(show _).permission(s"${aaa.id}.command.show").build(), "show", "s")
      .child(CommandSpec.builder
        .arguments(flags().flag("-global").buildWith(allOf(new TemplateCommandElement(Text.of("template-to-append"), () => config.keys))))
        .executor(apply _).permission(s"${aaa.id}.command.apply").build(), "apply", "a")
      .child(CommandSpec.builder
        .arguments(flags().flag("-global").buildWith(allOf(new TemplateCommandElement(Text.of("template-to-remove"), () => config.keys))))
        .executor(unapply _).permission(s"${aaa.id}.command.unapply").build(), "unapply", "u")
      .executor(fallback _).build(), "aaa", aaa.id)
    ()
  }
}
