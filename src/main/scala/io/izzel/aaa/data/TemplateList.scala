package io.izzel.aaa.data

import java.util.{Collections, Optional}

import com.google.common.reflect.TypeToken
import io.izzel.aaa
import org.spongepowered.api.data.key.Key
import org.spongepowered.api.data.manipulator.immutable.common.{AbstractImmutableListData => AILData}
import org.spongepowered.api.data.manipulator.mutable.common.{AbstractListData => ALData}
import org.spongepowered.api.data.manipulator.{DataManipulatorBuilder => DMBuilder}
import org.spongepowered.api.data.merge.MergeFunction
import org.spongepowered.api.data.persistence.AbstractDataBuilder
import org.spongepowered.api.data.value.mutable.ListValue
import org.spongepowered.api.data.{DataContainer, DataHolder, DataView, _}

import scala.collection.JavaConverters._
import scala.collection.{TraversableOnce, immutable, mutable}

object TemplateList {
  final val version = 0

  final val id = s"${aaa.id}:templates"

  final val name = s"${aaa.name}Templates"

  final val query: DataQuery = DataQuery.of(name)

  final val key: Key[ListValue[Value]] = {
    val token = new TypeToken[ListValue[Value]]() {}
    val (keyId, keyName) = ("aaa_templates", "AAATemplates")
    Key.builder.`type`[java.util.List[Value], ListValue[Value]](token).id(keyId).name(keyName).query(query).build()
  }

  case class Value(template: String)

  class Data(list: mutable.Buffer[Value]) extends ALData[Value, Data, ImmutableData](key, list.asJava) {
    override def asImmutable: ImmutableData = new ImmutableData(list.to[immutable.List])

    override def fill(dataHolder: DataHolder, overlap: MergeFunction): Optional[Data] = {
      val original = dataHolder.get(classOf[Data]).orElse(null);
      val data = overlap.merge(this, original)
      data.replace(list)
      Optional.of(data)
    }

    override def from(view: DataContainer): Optional[Data] = {
      replace(view.getStringList(query).orElse(Collections.emptyList[String]).asScala.map(Value))
      Optional.of(this)
    }

    override def copy: Data = new Data(list.clone)

    override def getContentVersion: Int = version

    private def replace(t: TraversableOnce[Value]): Unit = {
      list.clear()
      list ++= t
    }
  }

  class ImmutableData(list: immutable.List[Value]) extends AILData[Value, ImmutableData, Data](key, list.asJava) {
    override def asMutable: Data = new Data(list.to[mutable.Buffer])

    override def fillContainer(view: DataContainer): DataContainer = {
      super.fillContainer(view).set(query, list.map(_.template).asJava)
    }

    override def getContentVersion: Int = version
  }

  object DataBuilder extends AbstractDataBuilder[Data](classOf[Data], version) with DMBuilder[Data, ImmutableData] {
    override def buildContent(view: DataView): Optional[Data] = {
      Optional.of(new Data(view.getStringList(query).orElse(Collections.emptyList[String]).asScala.map(Value)))
    }

    override def createFrom(dataHolder: DataHolder): Optional[Data] = {
      Optional.of(new Data(dataHolder.get(key).orElse(Collections.emptyList[Value]).asScala))
    }

    override def create: Data = new Data(mutable.Buffer())
  }
}
