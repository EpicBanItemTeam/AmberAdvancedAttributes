package io.izzel.aaa.slot

import io.izzel.aaa
import io.izzel.aaa.api.data.{Template, TemplateSlot}
import org.spongepowered.api.Sponge
import org.spongepowered.api.service.permission.{PermissionService, SubjectData}

import scala.collection.JavaConverters._

class GlobalSlot extends TemplateSlot.Global {
  override def asTemplate(): Template = Template.parse("global")

  override def getTemplates: java.util.List[_ <: Template] = {
    val subject = Sponge.getServiceManager.provideUnchecked(classOf[PermissionService]).getDefaults
    val metaString = subject.getOption(SubjectData.GLOBAL_CONTEXT, aaa.templateKey).orElse("")
    metaString.split('|').filter("[a-z0-9_-]+".matches).map(Template.parse).toList.asJava
  }

  override def setTemplates(list: java.util.List[_ <: Template]): Unit = {
    val subject = Sponge.getServiceManager.provideUnchecked(classOf[PermissionService]).getDefaults
    subject.getTransientSubjectData.setOption(SubjectData.GLOBAL_CONTEXT, aaa.templateKey, list.asScala.mkString("|"))
  }
}
