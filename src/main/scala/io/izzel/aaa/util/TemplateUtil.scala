package io.izzel.aaa.util

import java.util.Collections

import io.izzel.aaa.data.TemplateList._
import org.spongepowered.api.item.inventory.{ItemStack, ItemStackSnapshot}
import org.spongepowered.api.service.permission.{PermissionService, SubjectData}
import org.spongepowered.api.{Server, Sponge}

import scala.collection.JavaConverters._

object TemplateUtil {

  // noinspection DuplicatedCode
  implicit class ItemStackToTemplates(item: ItemStack) {
    def templates: Option[List[Value]] = if (!item.supports(key)) None else {
      Some(item.get(key).orElse(Collections.emptyList[Value]).asScala.toList)
    }

    def templates_=(option: Option[List[Value]]): Unit = option match {
      case None => item.remove(key)
      case Some(list) => item.offer(key, list.asJava)
    }
  }

  // noinspection DuplicatedCode
  implicit class ItemStackSnapshotToTemplates(snapshot: ItemStackSnapshot) {
    def templates: Option[List[Value]] = if (!snapshot.supports(key)) None else {
      Some(snapshot.get(key).orElse(Collections.emptyList[Value]).asScala.toList)
    }
  }

  // noinspection ScalaUnusedSymbol
  implicit class ServerToTemplates(server: Server) {
    def templates: Option[List[Value]] = {
      val options = serverData.getOptions(SubjectData.GLOBAL_CONTEXT)
      if (!options.containsKey("aaa-templates")) None else {
        Some(options.get("aaa-templates").split('|').map(Value).toList)
      }
    }

    def templates_=(option: Option[List[Value]]): Unit = option match {
      case None => serverData.setOption(SubjectData.GLOBAL_CONTEXT, "aaa-templates", null)
      case Some(list) => serverData.setOption(SubjectData.GLOBAL_CONTEXT, "aaa-templates", list.mkString("|"))
    }

    private def serverData = {
      val service = Sponge.getServiceManager.provideUnchecked(classOf[PermissionService])
      service.getDefaults.getTransientSubjectData
    }
  }
}
