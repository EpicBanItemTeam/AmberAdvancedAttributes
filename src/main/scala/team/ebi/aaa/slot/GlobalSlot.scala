package team.ebi.aaa.slot

import java.util.Collections

import team.ebi.aaa
import team.ebi.aaa.api.data.{Template, TemplateSlot}
import team.ebi.aaa.util._
import org.spongepowered.api.Sponge
import org.spongepowered.api.service.permission.{Subject, SubjectData}
import team.ebi.aaa.api.data.TemplateSlot.Global

class GlobalSlot extends Global {
  private final val defStr = "global"

  private def subject: Subject = Sponge.getServer.getConsole

  override def toString: String = s"GlobalSlot{${subject.getIdentifier}}"

  override def asTemplate: Template = Template.parse(defStr)

  override def getTemplates: java.util.List[_ <: Template] = {
    Option(subject.getTransientSubjectData.getOptions(SubjectData.GLOBAL_CONTEXT).get(aaa.templateKey)) match {
      case Some(meta) => meta.split('|').filter(_.matches("[a-z][a-z0-9_-]*")).map(Template.parse).toList.asJava
      case None => locally {
        subject.getTransientSubjectData.setOption(SubjectData.GLOBAL_CONTEXT, aaa.templateKey, defStr)
        Collections.singletonList(Template.parse(defStr))
      }
    }
  }

  override def setTemplates(list: java.util.List[_ <: Template]): Unit = {
    subject.getTransientSubjectData.setOption(SubjectData.GLOBAL_CONTEXT, aaa.templateKey, list.asScala.mkString("|"))
  }
}
