package io.izzel.aaa.service;

import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Map;

/**
 * @author ustc_zzzz
 */
@FunctionalInterface
public interface AttributeToLoreFunction<T extends DataSerializable> {
    List<Map.Entry<Byte, Text>> toLoreTexts(List<? extends T> values);
}
