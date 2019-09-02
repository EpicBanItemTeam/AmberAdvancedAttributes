
#### `aaa-init`

初始化命令，只有初始化后的物品才可以添加属性。由 AmberAdvancedAttributes 接管的物品 lore 文本由插件生成。

#### `aaa-drop`

将物品标记为非 AmberAdvancedAttributes 物品，物品将失去所有属性，lore 文本可以更改。

#### `aaa-<属性>`

更改物品的属性

#### `aaa-<属性> <append|prepend|insert> <小值> [to <大值>] [at <位置>]`

添加属性。`append` 在尾部添加，`prepend` 在头部添加，`insert` 在指定地方添加。

#### `aaa-<属性> clear`

删除属性

#### `aaa-<标记属性> <mark|unmark>`

添加或删除标记属性

#### `aaa-possess [玩家]`

将物品标记主人为持有者，其他玩家无法获得该物品

#### `aaa-publicize`

将物品标记的主人删除

#### `aaa-items <give|save|name>` :id=items

* `/aaa-items give <id>` 给玩家某 id 的物品
* `/aaa-items save <id>` 将物品保存为某个 id，以用作[模板](/zh-cn/attributes.md#template)，[套装](/zh-cn/attributes.md#suit)或[镶嵌](/zh-cn/attributes.md#inlay)
* `/aaa-items name <物品名称>` 设置物品显示的名称，该名称将作为套装名称显示的文本
