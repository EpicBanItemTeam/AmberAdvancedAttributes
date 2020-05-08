package team.ebi.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import org.spongepowered.api.Sponge
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.text.chat.ChatTypes
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._

@Singleton
class HelloAttribute @Inject()(manager: AttributeManager)(implicit container: PluginContainer) extends Attribute[String] {
  override def getDeserializationKey: String = "aaa-hello"

  override def getDataClass: Class[String] = classOf[String]

  override def isCompatibleWith(slot: TemplateSlot): Boolean = slot.isInstanceOf[TemplateSlot.Global]

  listenTo[ClientConnectionEvent.Join] { event =>
    val player = event.getTargetEntity
    val channel = Sponge.getServer.getBroadcastChannel
    for (mappings <- manager.collectMappings(player).values.asScala) {
      for (textString <- Mappings.flattenDataStream(mappings, this).iterator.asScala) {
        channel.send(deserializeText(textString, player), ChatTypes.SYSTEM)
      }
    }
  }
}
