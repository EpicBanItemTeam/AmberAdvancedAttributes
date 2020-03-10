package io.izzel

import com.google.inject.Inject
import io.izzel.amber.commons.i18n.AmberLocale
import org.spongepowered.api.plugin.{Dependency, Plugin, PluginContainer}
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

package object aaa {
  final val id = "amber" + "advanced" + "attributes"
  final val name = "Amber" + "Advanced" + "Attributes"
  final val dependency = "scala" + "dependency" + "loader"

  @Plugin(id = id, name = name, description = name, dependencies = Array(new Dependency(id = dependency)))
  class AmberAdvancedAttributes @Inject()(container: PluginContainer, locale: AmberLocale) {
    locale.log("log.hello-world",
      Text.of(TextColors.LIGHT_PURPLE, name),
      Text.of(TextColors.LIGHT_PURPLE, container.getVersion.orElse("unknown")))
  }
}
