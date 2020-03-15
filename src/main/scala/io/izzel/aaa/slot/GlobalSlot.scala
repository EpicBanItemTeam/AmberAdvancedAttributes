package io.izzel.aaa.slot

import java.util.Collections

import io.izzel.aaa
import io.izzel.aaa.api.data.{Template, TemplateSlot}
import org.spongepowered.api.Sponge
import org.spongepowered.api.service.permission.{PermissionService, SubjectData}

import scala.collection.JavaConverters._

class GlobalSlot extends TemplateSlot.Global {
  private final val defStr = "global"

  override def asTemplate(): Template = Template.parse(defStr)

  override def getTemplates: java.util.List[_ <: Template] = {
    val subject = Sponge.getServiceManager.provideUnchecked(classOf[PermissionService]).getDefaults
    Option(subject.getOption(SubjectData.GLOBAL_CONTEXT, aaa.templateKey).orElse(null)) match {
      case Some(meta) => meta.split('|').filter("[a-z0-9_-]+".matches).map(Template.parse).toList.asJava
      case None => locally {
        subject.getTransientSubjectData.setOption(SubjectData.GLOBAL_CONTEXT, aaa.templateKey, defStr)
        Collections.singletonList(Template.parse(defStr))
      }
    }
  }

  override def setTemplates(list: java.util.List[_ <: Template]): Unit = {
    val subject = Sponge.getServiceManager.provideUnchecked(classOf[PermissionService]).getDefaults
    subject.getTransientSubjectData.setOption(SubjectData.GLOBAL_CONTEXT, aaa.templateKey, list.asScala.mkString("|"))
  }
}
