package io.izzel.aaa.config

import java.io.{BufferedReader, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.util.concurrent.Callable
import java.util.function.Consumer

import com.google.inject.{Inject, Provider, Singleton}
import io.izzel.aaa
import io.izzel.aaa.api.context.{ContextualTransformer, InitializationContext}
import io.izzel.aaa.api.data.Template
import io.izzel.aaa.api.data.visitor.TemplatesVisitor
import io.izzel.aaa.attribute.AttributeManager
import io.izzel.aaa.util._
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.event.game.state.GameStartedServerEvent
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.scheduler.Task

import scala.collection.mutable
import scala.util.continuations.reset
import scala.util.{DynamicVariable, Try}

@Singleton
class ConfigManager @Inject()(implicit container: PluginContainer,
                              attributes: Provider[AttributeManager],
                              logger: Logger, @ConfigDir(sharedRoot = false) configDir: Path) {
  type Initializer = ContextualTransformer[InitializationContext, TemplatesVisitor]

  private val loader: HoconConfigurationLoader = HoconConfigurationLoader.builder.setSource(Executor).build()

  private val templateToGetters: mutable.Map[Template, Map[String, Initializer]] = mutable.HashMap()

  private object Executor extends Consumer[Task] with Callable[BufferedReader] {

    import StandardWatchEventKinds._

    private val file: DynamicVariable[Path] = new DynamicVariable[Path](null)

    private val watchPath = {
      val subDir = Paths.get("templates")
      Files.createDirectories(configDir.resolve(subDir))
    }

    private val key: WatchKey = {
      val watcher = FileSystems.getDefault.newWatchService()
      watchPath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    }

    private def getTemplateByName(path: Path): Option[Template] = {
      Try(Template.parse(path.getFileName.toString.ensuring(_.endsWith(".conf")).dropRight(5))).toOption
    }

    private def loadTemplateConfig(path: Path, template: Template): Unit = {
      val builder = Map.newBuilder[String, Initializer]
      val node = file.withValue(watchPath.resolve(path))(loader.load().getNode(aaa.templateKey))
      for ((key, attr) <- attributes.get.attributeMap; childNode = node.getNode(key) if !childNode.isVirtual) {
        builder += key -> attr.initAttributes(childNode)
      }
      templateToGetters.put(template, builder.result)
    }

    private def reload(): Unit = {
      val affected = {
        val affectedBuilder = Set.newBuilder[Template]
        for (e <- key.pollEvents.asScala; path = e.context.asInstanceOf[Path]) e.kind match {
          case OVERFLOW => ()
          case ENTRY_DELETE => for (template <- getTemplateByName(path)) {
            templateToGetters.remove(template)
            affectedBuilder += template
          }
          case ENTRY_CREATE | ENTRY_MODIFY => for (template <- getTemplateByName(path)) try {
            logger.info(s"Hot reloading ${path.getFileName} (to template{$template}) ...")
            loadTemplateConfig(path, template)
            affectedBuilder += template
          } catch {
            case e: IOException => logger.error(s"An error was found when hot reloading ${watchPath.resolve(path)}", e)
          }
        }
        affectedBuilder.result
      }
      if (affected.nonEmpty) Sponge.getEventManager.post(new ConfigReloadEvent(affected))
    }

    override def call(): BufferedReader = Files.newBufferedReader(file.value, StandardCharsets.UTF_8)

    override def accept(t: Task): Unit = if (key.reset()) reload() else t.cancel()

    reset {
      waitFor[GameStartedServerEvent]
      val affected = {
        val affectedBuilder = Set.newBuilder[Template]
        Task.builder.delayTicks(1).intervalTicks(1).execute(Executor).submit(container)
        for (path <- Files.list(watchPath).iterator.asScala; template <- getTemplateByName(path)) try {
          logger.info(s"Start loading ${path.getFileName} (to template{$template}) ...")
          loadTemplateConfig(path, template)
          affectedBuilder += template
        } catch {
          case e: IOException => logger.error(s"An error was found when start loading ${watchPath.resolve(path)}", e)
        }
        affectedBuilder.result
      }
      if (affected.nonEmpty) Sponge.getEventManager.post(new ConfigReloadEvent(affected))
      ()
    }
  }

  def keys: Iterable[Template] = templateToGetters.keySet

  def get(template: Template): Option[Map[String, Initializer]] = templateToGetters.get(template)
}
