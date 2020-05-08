package team.ebi.aaa.attribute.event

import com.google.common.collect.Lists
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.event.cause.Cause
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.util._

import scala.collection.{immutable, mutable}

class AttributeLoadEvent(logger: Logger) extends Attribute.LoadEvent {
  // reverse the list since add(0, list) is called quite frequently so that it will be added as last element actually
  private val list: java.util.List[Attribute[_]] = Lists.reverse(new java.util.ArrayList)

  private val currentCause: Cause = Sponge.getCauseStackManager.getCurrentCause

  override def getAttributesToBeRegistered: java.util.List[Attribute[_]] = list

  override def getCause: Cause = currentCause

  def build(): immutable.ListMap[String, Attribute[_]] = try {
    val attributeBuilder = mutable.ListMap.empty[String, Attribute[_]]
    for (attribute <- list.asScala; key = attribute.getDeserializationKey) attributeBuilder.get(key) match {
      case None => attributeBuilder.put(attribute.getDeserializationKey, attribute)
      case Some(_) => logger.warn(s"Duplicate deserialization key: $key (only the first registered will take effect)")
    }
    immutable.ListMap(attributeBuilder.toSeq: _*)
  } finally {
    list.clear()
  }
}
