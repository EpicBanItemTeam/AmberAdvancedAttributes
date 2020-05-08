package team.ebi.aaa.data

import java.util.{Optional, UUID}

import team.ebi.aaa
import team.ebi.aaa.api.data.Template
import team.ebi.aaa.util._
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData
import org.spongepowered.api.data.merge.MergeFunction
import org.spongepowered.api.data.persistence.{AbstractDataBuilder, InvalidDataException}
import org.spongepowered.api.data.{DataContainer, DataHolder, DataQuery, DataView}
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.serializer.{TextSerializer, TextSerializers}

import scala.collection.{immutable, mutable}

object CustomTemplates {
  final val version = 0

  final val id = "templates"

  final val name = s"${aaa.name}Templates"

  case class Backup(name: Option[Text], lore: Option[List[Text]])

  case class Value(uuid: UUID, backup: Backup, templates: List[Template])

  class Data private[CustomTemplates] (var value: Value, val extra: mutable.Map[DataQuery, ConfigurationNode]) extends AbstractData[Data, ImmutableData] with Equals {
    override def registerGettersAndSetters(): Unit = ()

    override def fillContainer(view: DataContainer): DataContainer = fillData(super.fillContainer(view), value, extra)

    override def fill(dataHolder: DataHolder, overlap: MergeFunction): Optional[Data] = {
      val original = dataHolder.get(classOf[Data]).asScala.get
      val originalData = overlap.merge(this, original)
      value = originalData.value
      Optional.of(this)
    }

    override def from(view: DataContainer): Optional[Data] = fromData(view, this).asJava

    override def asImmutable: ImmutableData = new ImmutableData(value, extra.toMap)

    override def copy: Data = new Data(value, extra.clone())

    override def getContentVersion: Int = version

    override def canEqual(other: Any): Boolean = other.isInstanceOf[Data]

    override def equals(other: Any): Boolean = other match {
      case that: Data => that.canEqual(this) && value == that.value && extra == that.extra
      case _ => false
    }

    override def hashCode(): Int = value.## * 31 + extra.##
  }

  class ImmutableData private[CustomTemplates] (val value: Value, val extra: immutable.Map[DataQuery, ConfigurationNode]) extends AbstractImmutableData[ImmutableData, Data] with Equals {
    override def registerGetters(): Unit = ()

    override def fillContainer(view: DataContainer): DataContainer = fillData(super.fillContainer(view), value, extra)

    override def asMutable: Data = new Data(value, extraFrom(extra))

    override def getContentVersion: Int = version

    override def canEqual(other: Any): Boolean = other.isInstanceOf[ImmutableData]

    override def equals(other: Any): Boolean = other match {
      case that: ImmutableData => that.canEqual(this) && value == that.value && extra == that.extra
      case _ => false
    }

    override def hashCode(): Int = value.## * 31 + extra.##
  }

  object Builder extends AbstractDataBuilder[Data](classOf[Data], version) with DataManipulatorBuilder[Data, ImmutableData] {
    override def buildContent(view: DataView): Optional[Data] = fromData(view, create()).asJava

    override def create(): Data = new Data(Value(new UUID(0, 0), Backup(None, None), Nil), extraFrom(Nil))

    override def createFrom(dataHolder: DataHolder): Optional[Data] = this.create().fill(dataHolder)
  }

  private val formattingCode: TextSerializer = TextSerializers.FORMATTING_CODE

  private val configurationLoader: HoconConfigurationLoader = HoconConfigurationLoader.builder.build()

  private def extraFrom(extra: Iterable[(DataQuery, ConfigurationNode)]): mutable.Map[DataQuery, ConfigurationNode] = {
    mutable.LinkedHashMap(extra.toSeq: _*).withDefault(_ => configurationLoader.createEmptyNode())
  }

  private def fillData(view: DataContainer, data: Value, extra: Iterable[(DataQuery, ConfigurationNode)]): DataContainer = {
    locally {
      view.remove(DataQuery.of("ExtraData"))
      for ((k, v) <- extra) view.set(DataQuery.of("ExtraData").`then`(k), v.getValue)
    }
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
    val rawExtra = view.getView(DataQuery.of("ExtraData")).asScala
    val rawTemplates = view.getStringList(DataQuery.of("Templates")).asScala
    val rawBackupName = view.getString(DataQuery.of("BackupDisplayName")).asScala
    val rawBackupLore = view.getStringList(DataQuery.of("BackupItemLore")).asScala

    val extra = rawExtra.map(_.getValues(false).asScala)
    val name = rawBackupName.map(formattingCode.deserialize)
    val lore = rawBackupLore.map(_.asScala.map(formattingCode.deserialize).toList)
    val templates = rawTemplates.map(_.asScala.filter(_.matches("[a-z][a-z0-9_-]*")).map(Template.parse).toList).getOrElse(Nil)

    data.extra.clear()
    data.value = Value(new UUID(0, 0), Backup(name, lore), templates)
    extra.foreach(data.extra ++= _.mapValues(configurationLoader.createEmptyNode().setValue(_)))
    Some(data)
  } catch {
    case _: InvalidDataException => None
  }
}
