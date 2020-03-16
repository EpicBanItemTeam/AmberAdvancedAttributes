package io.izzel.aaa.data

import java.util.{Collections, Optional, UUID}

import io.izzel.aaa
import io.izzel.aaa.api.data.Template
import io.izzel.aaa.util._
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData
import org.spongepowered.api.data.merge.MergeFunction
import org.spongepowered.api.data.persistence.{AbstractDataBuilder, InvalidDataException}
import org.spongepowered.api.data.{DataContainer, DataHolder, DataQuery, DataView}
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.serializer.{TextSerializer, TextSerializers}

object CustomTemplates {
  final val version = 0

  final val id = s"${aaa.id}:templates"

  final val name = s"${aaa.name}Templates"

  case class Backup(name: Option[Text], lore: Option[List[Text]])

  case class Value(uuid: UUID, backup: Backup, templates: List[Template])

  class Data(var value: Value) extends AbstractData[Data, ImmutableData] {
    override def registerGettersAndSetters(): Unit = ()

    override def fillContainer(view: DataContainer): DataContainer = fillData(super.fillContainer(view), this.value)

    override def fill(dataHolder: DataHolder, overlap: MergeFunction): Optional[Data] = {
      val original = dataHolder.get(classOf[Data]).asScala.get
      val originalData = overlap.merge(this, original)
      value = originalData.value
      Optional.of(this)
    }

    override def from(view: DataContainer): Optional[Data] = fromData(view, this).asJava

    override def asImmutable: ImmutableData = new ImmutableData(value)

    override def copy: Data = new Data(value)

    override def getContentVersion: Int = version
  }

  class ImmutableData(val value: Value) extends AbstractImmutableData[ImmutableData, Data] {
    override def registerGetters(): Unit = ()

    override def fillContainer(view: DataContainer): DataContainer = fillData(super.fillContainer(view), value)

    override def asMutable: Data = new Data(value)

    override def getContentVersion: Int = version
  }

  object Builder extends AbstractDataBuilder[Data](classOf[Data], version) with DataManipulatorBuilder[Data, ImmutableData] {
    override def buildContent(view: DataView): Optional[Data] = fromData(view, create()).asJava

    override def create(): Data = new Data(Value(new UUID(0, 0), Backup(None, None), Nil))

    override def createFrom(dataHolder: DataHolder): Optional[Data] = this.create().fill(dataHolder)
  }

  private val formattingCode: TextSerializer = TextSerializers.FORMATTING_CODE

  private def fillData(view: DataContainer, data: Value): DataContainer = {
    data.templates match {
      case Nil => view.remove(DataQuery.of("Templates"))
      case templates => view.set(DataQuery.of("Templates"), templates.map(_.toString).asJava)
    }
    data.backup.name match {
      case None => view.remove(DataQuery.of("BackupDisplayName"))
      case Some(texts) => view.set(DataQuery.of("BackupDisplayName"), formattingCode.serialize(texts))
    }
    data.backup.lore match {
      case None => view.remove(DataQuery.of("BackupItemLore"))
      case Some(texts) => view.set(DataQuery.of("BackupItemLore"), texts.map(formattingCode.serialize).asJava)
    }
  }

  private def fromData(view: DataView, data: Data): Option[Data] = try {
    val rawTemplates = view.getStringList(DataQuery.of("Templates")).asScala
    val rawBackupName = view.getString(DataQuery.of("BackupDisplayName")).asScala
    val rawBackupLore = view.getStringList(DataQuery.of("BackupItemLore")).asScala

    val name = rawBackupName.map(formattingCode.deserialize)
    val lore = rawBackupLore.map(_.asScala.map(formattingCode.deserialize).toList)
    val templates = rawTemplates.map(_.asScala.filter("[a-z0-9_-]+".matches).map(Template.parse).toList).getOrElse(Nil)

    data.value = Value(new UUID(0, 0), Backup(name, lore), templates)
    Some(data)
  } catch {
    case _: InvalidDataException => None
  }
}
