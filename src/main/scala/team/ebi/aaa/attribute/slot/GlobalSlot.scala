package team.ebi.aaa.attribute.slot

import java.util.Collections

import org.spongepowered.api.Sponge
import org.spongepowered.api.service.permission.{Subject, SubjectData}
import team.ebi.aaa
import team.ebi.aaa.api.data.{Template, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._

class GlobalSlot(manager: AttributeManager) extends TemplateSlot.Global {
  private final val defStr = "global"

  private val subject: Subject = Sponge.getServer.getConsole.tap(manager.checkPersistence)

  override def toString: String = s"GlobalSlot{${subject.getIdentifier}}"

  override def asTemplate: Template = Template.parse(defStr)

  override def getTemplates: java.util.List[_ <: Template] = {
    Option(subject.getSubjectData.getOptions(SubjectData.GLOBAL_CONTEXT).get(aaa.templateKey)) match {
      case Some(meta) => meta.split('|').flatMap(Template.tryParse(_).asScala).toList.asJava
      case None => locally {
        subject.getSubjectData.setOption(SubjectData.GLOBAL_CONTEXT, aaa.templateKey, defStr).join()
        Collections.singletonList(Template.parse(defStr))
      }
    }
  }

  override def setTemplates(list: java.util.List[_ <: Template]): Unit = {
    subject.getSubjectData.setOption(SubjectData.GLOBAL_CONTEXT, aaa.templateKey, list.asScala.mkString("|")).join()
  }
}
