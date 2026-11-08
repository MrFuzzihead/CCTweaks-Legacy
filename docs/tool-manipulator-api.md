# Tool Manipulator peripheral

The **Tool Manipulator** (the advanced Tool Host turtle upgrade) is exposed as a peripheral of type
`tool_manipulator` on the side the upgrade was installed. It performs the same actions a player
would perform with the item in the turtle's selected slot, and reports what happened.

Every method is called on the peripheral itself:

```lua
local tool = peripheral.find("tool_manipulator")
```

## Methods

| Method | Arguments | Result |
|--------|-----------|--------|
| `use()` / `useUp()` / `useDown()` | `duration?: number`, `sneak?: boolean` | Right-click in front / above / below |
| `canUse()` / `canUseUp()` / `canUseDown()` | – | `true` if a `use` would do something |
| `swing()` / `swingUp()` / `swingDown()` | `sneak?: boolean` | Left-click in front / above / below |
| `canSwing()` / `canSwingUp()` / `canSwingDown()` | – | `true` if a `swing` would do something |

`peripheral.find` picks either the left or right upgrade; use `peripheral.wrap(side)` to choose a
specific one when a turtle has two manipulators.

## Return values

`use` and the `swing` family return a tuple:

| Result | Meaning |
|--------|---------|
| `true, "item", "use"` | The item's special right-click behaviour ran (`onItemUseFirst`) |
| `true, "block", "interact"` | The block was activated (button pressed, GUI opened, …) |
| `true, "place"` | A block was placed |
| `true, "entity", …` | An entity was interacted with |
| `false, "<reason>"` | The action was refused, e.g. `"Cannot place protected block"`, `"Turtle action \"dig\" is disabled"` |
| `false` | Nothing was there to interact with |

`canUse`/`canSwing` return a single boolean. A `true` result does not guarantee the action succeeds
(block activation cannot be probed without side effects), but a `false` means it definitely will not.

## Arguments

* **`duration`** (default `0`) — how long, in ticks, the action is "held" after it runs. This is
  passed to the item's `onPlayerStoppedUsing`, so a bow or a tablet charges for that long, and it is
  capped at the item's own maximum use duration. The Lua call then **waits** that many ticks before
  returning (20 ticks = 1 second), so `use(40)` behaves like a two-second right-click.
* **`sneak`** (default `false`) — acts as if the player is holding shift. This is how you place
  blocks against blocks that have their own right-click action, and it is passed through to block
  activation and item use.

## Behaviour notes

* The turtle's selected slot provides the item. Tools work as tools: a sword swings for its full
  attack damage (including enchantments and modifiers), an axe chops, a shovel digs.
* Drops are collected into the turtle's inventory; only the overflow is left in the world, in front
  of the turtle.
* `dig`-speed is multiplied by `turtle.toolhost.digFactor` (default `10`) in `config/cctweaks.cfg`.
* Block protection is respected: `ComputerCraft.isBlockEditable` gates digging, placing and
  right-clicking, exactly as for CC's own turtle commands.
* Actions disabled in ComputerCraft's own config (`turtleDisabledActions`: `tool`, `dig`, `place`,
  `attack`, `swing`) also disable the corresponding peripheral methods.
* The upgrade is a turtle upgrade of type `Tool`, so normal `turtle.dig()`/`turtle.attack()` keep
  working alongside the peripheral.
