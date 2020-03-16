package io.izzel.aaa.data

import com.google.inject.{Inject, Singleton}
import io.izzel.aaa.util._
import org.slf4j.Logger
import org.spongepowered.api.data.DataRegistration
import org.spongepowered.api.event.game.state.GamePreInitializationEvent
import org.spongepowered.api.plugin.PluginContainer

import scala.util.continuations.reset

@Singleton
class CustomDataManager @Inject()(implicit container: PluginContainer, logger: Logger) {
  reset {
    waitFor[GamePreInitializationEvent]
    logger.info("Registering custom data ...")
    registerCustomTemplates()
  }

  private def registerCustomTemplates(): Unit = {
    import io.izzel.aaa.data.CustomTemplates._
    DataRegistration.builder.id(id).name(name)
      .dataClass[Data, ImmutableData](classOf[Data]).immutableClass(classOf[ImmutableData]).builder(Builder).build()
  }
}