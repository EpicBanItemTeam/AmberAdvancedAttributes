
!> Type `/aaa-` then press `Tab` to view all commands.

#### `attack` `pvp-attack` `pve-attack`

Increase damage to attacked entities.

#### `defense` `pvp-defense` `pve-defense`

Decrease damage when hurt, including environmental damage (falling/cactus/burn/etc）.

`damage = (1 / (1 + defense)) * original_damage`

#### `reflect` `pvp-reflect` `pve-reflect`

Reflect damage when attacked by entities.

#### `critical` `critical-rate`

Critical damage increment and critical damage rate.

#### `dodge` `accuracy`

Dodge rate and accuracy. With a `(dodge - accuracy)%` chance, an attack performs no damage.

#### `tracing`

Arrows follow targeted enemy.

#### `accelerate`

Arrows fly faster. (Not available at this time)

#### `attack-speed`

Decrease cooldown time after performing an attack.

#### `move-speed`

Increase move speed. Default value is `0.3`.

#### `durability`

Change durability value of the item, only available on items with durability.

Type `/aaa-durability append <current-durability> to <max-durability>` to set the durability.

#### `unbreakable`

Unbreakable item.

#### `loot-rate`

Randomly steal an item from the inventory of the attacked entities with given chance.

#### `loot-immune`

Item immune to be stolen. (Correspond to `loot-rate` attribute)

#### `burn` `burn-rate`

Ignite enemy with given time(tick) and given chance.

Default `burn-rate` is zero, so attribute affects only when set both attributes.

#### `life-steal` `life-steal-rate`

Life steal with given amount and given chance.

Default `life-steal-rate` is zero, so attribute affects only when set both attributes.

#### `max-health`

Increase max health, 2 point correspond to 1 heart. Default max health is `20`.

#### `attack-range`

Increase attack range. (Not available at this time)

#### `starvation` `saturation`

Starvation and saturation, increase or decrease food level with given amount per second.

#### `regeneration`

Increase health per second.

#### `knockback`

Increase knockback distance.

#### `instant-death` `instant-death-immune`

Attacked entities without `instant-death-immune` just died when `instant-death` affects.

#### `possession`

Set the player possess this item. So called soul bind.

Entity with `amberadvancedattributes.possession-bypass` permission do not affect by this.

#### `equipment`

Set the attributes of the item affect only in specific slots (`main_hand`, `off_hand`, `boots`, `chestplate`, `head`, `leggings`)

当对应物品被用作套装时，该属性变为须集齐某些套装才可使套装生效

!> 理论上套装和模板可以无限嵌套，但是不要设置某个物品将自身作为模板

!> 套装和模板可以随时更改并实时生效到已发放的物品上

#### `suit`

设置物品对应的套装（可以有多个）

设置套装的方法如下
* 将任意物品添加需要的套装加成效果
* 给套装效果物品添加 `equipment` 属性标记套装的数量，如 `/aaa-equipment mark main_hand off_hand` 指明套装效果需要主手副手同时持有才生效
* 使用 [items](/en-us/commands.md#items) 系列指令 **设置套装显示名称** 并将其保存为某个 id
* 使用 `/aaa-suit append <id>` 命令给对应物品标记为套装

#### `template`

设置物品的模板（可以有多个），物品的属性继承自其模板

使用 `/aaa-template append xxxx as hidden` 使模板的 lore 不显示在物品上，但模板物品的属性仍然生效

设置物品模板的方式与套装类似

#### `custom-lore`

设置物品的自定义描述，单独显示一片文本

#### `permission-cap`

设置物品的权限要求

可自定义权限的显示：

* 打开 `locale_xx_xx.conf` 语言文件
* 找到 
    ```hocon
    attributes {
      permission-cap {
        mappings {
        }
      }
    }
    ```
* 在其中添加如
    ```hocon
    mappings {
      plugin.vip = "VIP"
    }
    ```
  的 `权限 = 自定义文本` 列表

#### `level-cap`

等级限制。

设置单个数值时玩家等级须大于等于设置的等级，设置两个数值时玩家等级须在两个值的闭区间内。

#### `inlay` `inlay-gem` `inlay-success` :id=inlay

镶嵌相关的属性。

设置镶嵌的方法如下：

* 对于被镶嵌的物品
    * 手持物品输入 `/aaa-inlay append id` 设置一个名为 `id` 的镶嵌槽
* 对于用于镶嵌的物品（宝石）
    * 手持物品输入 `/aaa-inlay-gem mark` 将其标记为宝石，此物品装备/手持时属性便不生效
    * 输入 `/aaa-inlay append id` 设置其能被镶嵌在某个槽位
    * 添加 `aaa-inlay-success` 属性设置镶嵌成功率，失败则物品（宝石）消失
* 主手持物品，副手持宝石，按 `F` （交换主副手物品）键镶嵌

