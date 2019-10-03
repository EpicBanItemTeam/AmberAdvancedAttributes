
!> 输入 `/aaa-`，按下 `Tab` 键，查看补全

#### `attack` `pvp-attack` `pve-attack`

增加对攻击的实体的伤害

#### `defense` `pvp-defense` `pve-defense`

减少收到伤害时的伤害值，包括环境伤害（如掉落/仙人掌/燃烧）

计算方法 `伤害 = (1 / (1 + 防御值)) * 原伤害`

#### `reflect` `pvp-reflect` `pve-reflect`

受到生物攻击时反弹的伤害量

#### `critical` `critical-rate`

暴击伤害加成数值及暴击几率

#### `dodge` `accuracy`

躲避几率和命中几率，有 `(躲避-命中)%` 的几率不造成伤害

#### `tracing`

箭矢追踪敌人，也就是所谓的追踪箭

#### `accelerate`

箭矢加速（暂时不可用）

#### `attack-speed`

减少攻击后冷却时间

#### `move-speed`

增加移动速度，默认值是 `0.3`

#### `durability`

更改物品耐久，仅支持支持耐久值的物品

使用命令 `/aaa-durability append 当前耐久 to 最大耐久` 设置耐久值

#### `unbreakable`

物品不可破坏，耐久使用时不减少

#### `loot-rate`

抢夺他人物品的几率（随机从对方背包内拿走一样物品）

#### `loot-immune`

物品免疫被抢夺

#### `burn` `burn-rate`

点燃敌人时间（单位为 tick）和点燃敌人的几率

默认几率是 0，所以需要同时设置两个属性才会生效

#### `life-steal` `life-steal-rate`

吸血量和吸血几率，攻击时将吸血量增加到自身的生命值中

默认几率是 0，所以需要同时设置两个属性才会生效

#### `max-health`

增加最大生命值，2 点为一颗心。默认最大生命为 `20`。

#### `attack-range`

增加玩家攻击范围（暂不可用）

#### `starvation` `saturation`

饥饿与饱和属性，分别是每秒减少或增加饥饿值

#### `regeneration`

每秒恢复血量

#### `knockback`

增加攻击时击退距离

#### `instant-death` `instant-death-immune`

一击必杀的几率，以及免疫一击必杀

#### `possession`

设置物品被玩家拥有，也就是所谓的灵魂绑定

拥有 `amberadvancedattributes.possession-bypass` 的实体不受影响

#### `equipment`

设置物品仅能在某些槽位生效（`main_hand`, `off_hand`, `boots`, `chestplate`, `head`, `leggings`）

当对应物品被用作套装时，该属性变为须集齐某些套装才可使套装生效

!> 理论上套装和模板可以无限嵌套，但是不要设置某个物品将自身作为模板

!> 套装和模板可以随时更改并实时生效到已发放的物品上

#### `suit`

设置物品对应的套装（可以有多个）

设置套装的方法如下
* 将任意物品添加需要的套装加成效果
* 给套装效果物品添加 `equipment` 属性标记套装的数量，如 `/aaa-equipment mark main_hand off_hand` 指明套装效果需要主手副手同时持有才生效
* 使用 [items](/zh-cn/commands.md#items) 系列指令 **设置套装显示名称** 并将其保存为某个 id
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

#### `potion-effect`

击中目标后应用药水效果

使用 `/aaa-potion-effect append <效果ID> <时长> [倍数]` 添加一种药水效果到装备

可以通过添加 `minecraft_server.jar` 内 `/assets/lang` 的语言文件实现效果名的汉化
