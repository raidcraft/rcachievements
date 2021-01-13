# RCAchievements

[![Build Status](https://github.com/raidcraft/rcachievements/workflows/Build/badge.svg)](../../actions?query=workflow%3ABuild)
[![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/raidcraft/rcachievements?include_prereleases&label=release)](../../releases)
[![codecov](https://codecov.io/gh/raidcraft/rcachievements/branch/master/graph/badge.svg)](https://codecov.io/gh/raidcraft/rcachievements)
[![Commitizen friendly](https://img.shields.io/badge/commitizen-friendly-brightgreen.svg)](http://commitizen.github.io/cz-cli/)
[![semantic-release](https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg)](https://github.com/semantic-release/semantic-release)

RCAchievements ist das Achievement Plugin des [Raid-Craft](https://raid-craft.de) Servers. Es ermöglicht die Erstellung diverser Achievements (Erfolge) durch Configs und Commands.

* [Configuration](#configuration)
* [Achievement Configuration](#achievement-configuration)
* [Achievement Types](#achievement-types)
  * [none](#none)
  * [block](#block)
  * [combined](#combined)
  * [location](#location)
  * [kill-entity](#kill-entity)
  * [craft](#craft)

## Configuration

The plugin has a very simple `config.yml` to configure the database connection and some defaults. The primary configuration takes place inside the `achievements/` folder where all of the achievements are configured.

```yaml
# the path to the achievements configs
# all configs in here are loaded recursively
achievements: achievements/
# the default achievement type to use if none is specified in the config
# see below for a list of inbuilt types
default_type: none
database:
  username: sa
  password: sa
  driver: h2
  url: "jdbc:h2:~/achievements.db"
```

## Achievement Configuration

Achievements can exist as configuration files or in the database only. It depends on how you created them. You can always save a database achievement to disk by running the `/rca:admin save <achievement>` command.

Create new achievement configs inside the `achievements` directory as `.yml` files. All achievements can have the following properties but require none of them by default.

> All configured achievements are stored in the database, regardless if they are loaded from configs or created with commands.  
> Just command created achievements do not exist as configs, automatically. They need to be saved: `/rca:admin save <achievement>`.

```yaml
# DO NOT CHANGE OR REMOVE THIS
# the id is automatically generated the first time your achievement is loaded.
# do not modify or remove it or else all players will lose this achievement.
# if you copy a config make sure to remove the id from the copy.
# id: de3d61fb-6c7c-4ca3-816b-7b7c8175522b

# a short and precise unique alias of the achievement used in commands.
# if no specified the alias is equal to the file name.
alias: my-first-achievement
# the type of the achievement defines how the achievement is processed and how the player can unlock it.
# the default type "none" can only be given manually to the player /rca:admin add <player> <achievement>
type: none
# a friendly name of the achievement that is displayed to the player
name: My Achievement
# a short and precise description of the achievement.
# can be obfuscated with the secret property.
description: Join our server the first time!
# set to false to disable the achievement
# player that unlocked it will keep the achievement but no new player can unlock it
enabled: true
# hidden achievements are only displayed once unlocked
hidden: false
# secret achievements have their name displayed but only players that unlocked it can read the description
secret: false
# set to false to disable broadcasting this achievement when a player unlocks it
broadcast: true
# set to true to require the rcachievements.achievement.<alias> permission to unlock the achievement
restricted: false
# the with section contains all config values required by the achievement type
# see the list below for built in achievement types and their config values
with:
  ...
```

## Achievement Types

The achievement type controls how an achievement is executed and obtained by the player. Each type comes with its own config values that are configured under the `with` section in the achievement config.

### none

The default achievement type that can only be unlocked when given via the `/rca:admin add <player> <achievement>` command.

### block

A `counter` achievement that tracks placing and breaking blocks.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `count` | `1` | How many times must the action execute until the achievement is unlocked. |
| `action` | `place` | The action that is tracked by the achievement. `place`, `break` |
| `blocks` | `[]` | A list of block materials that increase the counter. Can be any valid [minecraft item](https://minecraft-ids.grahamedgecombe.com/). The counter is shared between all block types. |

### combined

Use the combined achievement to give the player an achievement when he unlocked a combined list of other achievements.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `achievements` | `[]` | A list of achievements that the player needs to unlock this achievement. |

### location

Give the player an achievement when comes inside the radius of the defined location.
Can be created with the `/rca:admin create location <alias> <name> <description> [radius]` command at the current position.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `world` | `world` | The name of the world the location is in. |
| `x` |  | The x-coordinate of the location. |
| `y` |  | The y-coordinate of the location. |
| `z` |  | The z-coordinate of the location. |
| `radius`| `0` | The radius in blocks around the location that trigger the achievement. |

### kill-entity

Counts the killing of entities.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `count` | `1` | How many times must the action execute until the achievement is unlocked. |
| `entities` | `[]` | A list of entities that increase the counter. Can be any valid [minecraft entity](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html). The counter is shared between all entity types. |

### craft

Counts the number of crafted items. The counter is always increased by the amount of items crafted. Crafting 64 `minecraft:planks` will increase the counter by `64`.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `count` | `1` | How many items must be crafted until the achievement is unlocked. |
| `items` | `[]` | A list of items that increase the counter. Can be any valid [minecraft item](https://minecraft-ids.grahamedgecombe.com/). The counter is shared between all entity types. |
