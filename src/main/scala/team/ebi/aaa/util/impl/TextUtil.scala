package team.ebi.aaa.util.impl

import me.rojo8399.placeholderapi.PlaceholderService
import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.serializer.TextSerializers

trait TextUtil {
  def deserializeText(input: String, user: User): Text = {
    def replacePlaceholders(input: String, user: User): Text = {
      val placeholderService = Sponge.getServiceManager.provideUnchecked(classOf[PlaceholderService])
      val obj = if (user.getPlayer.isPresent) user.getPlayer.get else user
      placeholderService.replacePlaceholders(input, obj, obj)
    }
    val hasPlaceholderAPI = Sponge.getPluginManager.isLoaded("placeholder" + "api")
    if (hasPlaceholderAPI) replacePlaceholders(input, user) else TextSerializers.FORMATTING_CODE.deserialize(input)
  }
}
