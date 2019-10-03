
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

When `equipment` is used on suit items, it controls the amount of the suits to collect.

!> You can have nested suits and templates, but do not directly or indirectly set one's template to itself.

!> You can edit the attributes on templates and suits, and it will take effect on all items using it.

#### `suit`

Set a suit for an item. One item can have multiple suits.

To create a suit, you need:
* Add attributes to one item(the suit item that hold attributes, not the items you want to give to your player)
* Add `equipment` attribute to the suit item, like that `/aaa-equipment mark main_hand off_hand` means you need to have item marked with this suit in both mainhand and offhand,
    then the suit attributes will take effect.
* Use [items](/en-us/commands.md#items) commands to **set the display name** and save the suit item with specific `id`.
* Type `/aaa-suit append <id>` with items in hand to mark it belongs to this suit.

#### `template`

Set the template of an item. One item can have multiple templates, and the item inherits attributes from template.

Type `/aaa-template append xxxx as hidden` to set a hidden template that no lore displayed but attributes will take effect.

To set a template is almost the same as to set a suit.

#### `custom-lore`

Add custom descriptions to item.

#### `permission-cap`

Add permission requirements to item.

To customize permission display, you can:

* Open `locale_xx_xx.conf` file
* Find `attributes.permission-cap.mappings`
    ```hocon
    attributes {
      permission-cap {
        mappings {
        }
      }
    }
    ```
* Add
    ```hocon
    mappings {
      plugin.vip = "VIP"
    }
    ```
  with `Permission node = your text here` list.

#### `level-cap`

Add level requirements to item.

When set a single number, player's exp level should larger than given number.
When set a ranged number, player's exp level should be in the given closed interval.

#### `inlay` `inlay-gem` `inlay-success` :id=inlay

Inlay-related attributes.

To set a inlay attribute,

* For the item that holds the gem
    * Type `/aaa-inlay append id` to set a inlay slot named `id`
* For the gem
    * Type `/aaa-inlay-gem mark` to mark it as a gem, then attributes on it do not take effect.
    * Type `/aaa-inlay append id` to set the slots that accepts this gem.
    * Add `aaa-inlay-success` attributes to set the inlay success chance, when failed the gem disappeared.
* Hold the item in mainhand with gem in offhand, then press `F` (swap hand) to perform inlay action.

#### `potion-effect`

Apply potion effects when attacking target

Use `/aaa-potion-effect append <Type ID> <duration> [amplifier]` to add a potion effect
