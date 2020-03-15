package io.izzel.aaa.config

import java.io.{BufferedReader, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.util.concurrent.Callable
import java.util.function.Consumer

import com.google.inject.{Inject, Singleton}
import io.izzel.aaa
import io.izzel.aaa.api.data.Template
import io.izzel.aaa.util.EventUtil._
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.event.game.state.GameStartingServerEvent
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.scheduler.Task

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.DynamicVariable
import scala.util.continuations.reset
import scala.util.control.NonFatal

@Singleton
class ConfigManager @Inject()(implicit container: PluginContainer, logger: Logger, @ConfigDir(sharedRoot = false) configDir: Path) {
  private val loader: HoconConfigurationLoader = HoconConfigurationLoader.builder.setSource(Executor).build()

  private val nodes: mutable.Map[Template, ConfigurationNode] = mutable.HashMap()

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

    private def getTemplateByName(path: Path): Option[Template] = try {
      Some(Template.parse(path.getFileName.toString.ensuring(_.endsWith(".conf")).dropRight(5)))
    } catch {
      case NonFatal(_) => None
    }

    private def reload(): Unit = {
      val reloadingBuilder = Set.newBuilder[Template]
      for (e <- key.pollEvents.asScala; path = e.context.asInstanceOf[Path]) e.kind match {
        case OVERFLOW => ()
        case ENTRY_DELETE => for (template <- getTemplateByName(path)) {
          nodes.remove(template)
          reloadingBuilder += template
        }
        case ENTRY_CREATE | ENTRY_MODIFY => for (template <- getTemplateByName(path)) try {
          logger.info(s"Hot reloading ${path.getFileName} (to template{$template})...")
          nodes.put(template, file.withValue(watchPath.resolve(path))(loader.load().getNode(aaa.templateKey)))
          reloadingBuilder += template
        } catch {
          case e: IOException => logger.error(s"An error was found when hot reloading ${watchPath.resolve(path)}", e)
        }
      }
      Sponge.getEventManager.post(new ConfigReloadEvent(reloadingBuilder.result))
    }

    override def call(): BufferedReader = Files.newBufferedReader(file.value, StandardCharsets.UTF_8)

    override def accept(t: Task): Unit = if (key.reset()) reload() else t.cancel()

    reset {
      waitFor[GameStartingServerEvent]
      val affected = {
        val affectedBuilder = Set.newBuilder[Template]
        Task.builder.delayTicks(1).intervalTicks(1).execute(Executor).submit(container)
        for (path <- Files.list(watchPath).iterator.asScala; template <- getTemplateByName(path)) try {
          logger.info(s"Start loading ${path.getFileName} (to template{$template}) ...")
          nodes.put(template, file.withValue(watchPath.resolve(path))(loader.load().getNode(aaa.templateKey)))
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
  def get(template: Template): Option[ConfigurationNode] = nodes.get(template)
}
