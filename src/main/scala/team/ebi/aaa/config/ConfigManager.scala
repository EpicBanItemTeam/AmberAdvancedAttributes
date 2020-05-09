package team.ebi.aaa.config

import java.io.{BufferedReader, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.util.concurrent.Callable
import java.util.function.Consumer

import com.google.inject.{Inject, Provider, Singleton}
import io.izzel.amber.commons.i18n.AmberLocale
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.event.game.state.GameStartedServerEvent
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.scheduler.Task
import team.ebi.aaa
import team.ebi.aaa.api.context.{ContextualTransformer, InitializationContext}
import team.ebi.aaa.api.data.Template
import team.ebi.aaa.api.data.visitor.TemplatesVisitor
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._

import scala.collection.mutable
import scala.util.continuations.reset
import scala.util.control.NonFatal
import scala.util.{DynamicVariable, Try}

@Singleton
class ConfigManager @Inject()(implicit container: PluginContainer,
                              attributes: Provider[AttributeManager],
                              logger: Logger, locale: AmberLocale, @ConfigDir(sharedRoot = false) configDir: Path) {
  type Initializer = ContextualTransformer[InitializationContext, TemplatesVisitor]

  private val loader: HoconConfigurationLoader = HoconConfigurationLoader.builder.setSource(Executor).build()

  private val templateToGetters: mutable.Map[Template, Map[String, Initializer]] = mutable.HashMap()

  private object Executor extends Consumer[Task] with Callable[BufferedReader] {

    import StandardWatchEventKinds._

    private val file: DynamicVariable[Path] = new DynamicVariable[Path](null)

    private val watchPath = {
      val subDir = configDir.resolve(Paths.get("templates"))
      if (!Files.isDirectory(subDir)) {
        Files.createDirectories(subDir)
        val hasPlaceholderAPI = Sponge.getPluginManager.isLoaded("placeholder" + "api")
        val name = s"config/global_${if (hasPlaceholderAPI) "with" else "without"}_placeholder.conf"
        Sponge.getAssetManager.getAsset(container, name).get.copyToFile(subDir.resolve("global.conf"))
      }
      subDir
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

    private def logException(path: Path, e: Throwable): Unit = {
      val wrapped = if (e.isInstanceOf[IOException]) e else new IOException(e)
      logger.error(locale.getUnchecked("log.config.error-parsing", watchPath.resolve(path)).toPlain, wrapped)
    }

    private def reload(): Unit = {
      val affected = {
        val affectedBuilder = Set.newBuilder[Template]
        for (e <- key.pollEvents.asScala) e.kind match {
          case OVERFLOW => ()
          case ENTRY_DELETE => locally {
            val path = e.context.asInstanceOf[Path]
            for (template <- getTemplateByName(path)) {
              templateToGetters.remove(template)
              affectedBuilder += template
            }
          }
          case ENTRY_CREATE | ENTRY_MODIFY => locally {
            val path = e.context.asInstanceOf[Path]
            for (template <- getTemplateByName(path)) try {
              locale.log("log.config.hot-reloading", path.getFileName, template)
              loadTemplateConfig(path, template)
              affectedBuilder += template
            } catch {
              case NonFatal(e) => logException(path, e)
            }
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
          locale.log("log.config.start-loading", path.getFileName, template)
          loadTemplateConfig(path, template)
          affectedBuilder += template
        } catch {
          case NonFatal(e) => logException(path, e)
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
