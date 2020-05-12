package team.ebi.aaa.attribute.impl.traits

import team.ebi.aaa.api.Attribute
import team.ebi.aaa.data.DoubleRange

trait DoubleRangeAttribute extends Attribute[DoubleRange.Value[_]]{
  override def getDataClass: Class[DoubleRange.Value[_]] = classOf[DoubleRange.Value[_]]
}
