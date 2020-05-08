package team.ebi.aaa.config

import team.ebi.aaa.api.data.Template
import org.spongepowered.api.Sponge
import org.spongepowered.api.event.Event
import org.spongepowered.api.event.cause.Cause


class ConfigReloadEvent(val affected: Set[Template]) extends Event {
  private val spongeCurrentCause: Cause = Sponge.getCauseStackManager.getCurrentCause

  override def getCause: Cause = spongeCurrentCause
}
