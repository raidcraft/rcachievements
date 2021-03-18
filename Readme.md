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
  * [ART-Framework](#art-framework)
  * [block](#block)
  * [combined](#combined)
  * [location](#location)
  * [kill-entity](#kill-entity)
  * [craft](#craft)
  * [login](#login)
  * [statistic](#statistic)
  * [biomes](#biomes)
  * [money](#money)
* [SQL Statements](#sql-statements)

## Configuration

The plugin has a very simple `config.yml` to configure the database connection and some defaults. The primary configuration takes place inside the `achievements/` folder where all of the achievements are configured.

```yaml
# the path to the achievements configs
# all configs in here are loaded recursively
achievements: achievements/
# the default achievement type to use if none is specified in the config
# see below for a list of inbuilt types
default_type: none
# set to false to disable global broadcasting
broadcast: true
# how often (in ticks) should the global bungeecord player list update
player_list_update_interval: 200
# if the art-framework is enabled you can specify global rewards
# that are applied to all achievements.
# you can disable them in the individual achievment with the global_rewards: false flag.
global_rewards:
  - '!rcskills:exp.add 100'
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
# set to false to disable additional global rewards for the achievement
global_rewards: true
# a list of valid art-framework actions and requirement that are executed when the player gets the achievement
rewards:
  - '!money.add 100 "My Achievement"' # adds 100 coins to the player with the reason being the name of the achievement
  - '!rcskills:exp.add 1000' # adds 1000 rc-exp to the player
# the with section contains all config values required by the achievement type
# see the list below for built in achievement types and their config values
with:
  ...
```

## Achievement Types

The achievement type controls how an achievement is executed and obtained by the player. Each type comes with its own config values that are configured under the `with` section in the achievement config.

### none

The default achievement type that can only be unlocked when given via the `/rca:admin add <player> <achievement>` command.

### ART-Framework

One powerful feature of this achievement plugin is the ability to create achievements with the [art-framework](https://art-framework.io).

| Config | Default | Description |
| ------ | ------- | ----------- |
| `trigger` | `[]` | A list of [art-trigger](https://art-framework.io/#/configuration/trigger) in flow syntax. |
| `requirements` | `[]` | A list of [art-requirements](https://art-framework.io/#/configuration/requirements) that are checked after the trigger execution. |

### block

A `counter` achievement that tracks placing and breaking blocks.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `count` | `1` | How many times must the action execute until the achievement is unlocked. |
| `action` | `place` | The action that is tracked by the achievement. `place`, `break` |
| `blocks` | `[]` | A list of block materials that increase the counter. Can be any valid [minecraft item](https://minecraft-ids.grahamedgecombe.com/). The counter is shared between all block types. |
| `prefix` | `Fortschritt:` | The prefix that is written before the statistic count. |
| `suffix` | `Blöcke abgebaut/gesetzt` | A suffix that should be written after the statistic count in the overview, e.g. `<prefix> 10/100 <suffix>`. |

### combined

Use the combined achievement to give the player an achievement when he unlocked a combined list of other achievements.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `achievements` | `[]` | A list of achievements that the player needs to unlock this achievement. |
| `prefix` | `Fortschritt:` | The prefix that is written before the statistic count. |
| `suffix` | `Erfolge` | A suffix that should be written after the statistic count in the overview, e.g. `<prefix> 10/100 <suffix>`. |

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
| `prefix` | `Fortschritt:` | The prefix that is written before the statistic count. |
| `suffix` | `Mobs getötet` | A suffix that should be written after the statistic count in the overview, e.g. `<prefix> 10/100 <suffix>`. |

### craft

Counts the number of crafted items. The counter is always increased by the amount of items crafted. Crafting 64 `minecraft:planks` will increase the counter by `64`.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `count` | `1` | How many items must be crafted until the achievement is unlocked. |
| `items` | `[]` | A list of items that increase the counter. Can be any valid [minecraft item](https://minecraft-ids.grahamedgecombe.com/). The counter is shared between all entity types. |
| `prefix` | `Fortschritt:` | The prefix that is written before the statistic count. |
| `suffix` | `Items hergestellt` | A suffix that should be written after the statistic count in the overview, e.g. `<prefix> 10/100 <suffix>`. |

### login

Counts daily login streaks. The counter increases if the player logged in the same time in a row and resets on missed streaks. The `24:00` clock mark is used as day reference.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `count` | `1` | How often the player must login in a row until the achievement is unlocked. |
| `reset` | `true` | Resets the counter to `0` if the player misses a streak. |
| `prefix` | `Fortschritt:` | The prefix that is written before the statistic count. |
| `suffix` | `Tage` | A suffix that should be written after the statistic count in the overview, e.g. `<prefix> 10/100 <suffix>`. |

### statistic

Tracks any [minecraft statistic](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Statistic.html).  
A `material` must be specified if the `statistic` is one of the following: `DROP`, `PICKUP`, `MINE_BLOCK`, `USE_ITEM`, `BREAK_ITEM`, `CRAFT_ITEM`.  
A `entity` must be specified if the `statistic` is one of the following: `KILL_ENTITY`, `ENTITY_KILLED_BY`.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `count` | `1` | The statistic count until the achievement is unlocked. |
| `statistic` | | One of the statistics listed [here](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Statistic.html). |
| `material` | | Only required if `statistic` is one of the following: `DROP`, `PICKUP`, `MINE_BLOCK`, `USE_ITEM`, `BREAK_ITEM`, `CRAFT_ITEM` |
| `entity` | | Only required if `statistic` is one of the following: `KILL_ENTITY`, `ENTITY_KILLED_BY` |
| `prefix` | `Fortschritt:` | The prefix that is written before the statistic count. |
| `suffix` | | A suffix that should be written after the statistic count in the overview, e.g. `<prefix> 10/100 <suffix>`. |

### biomes

Keeps track of biomes the player visited. Defaults to all biomes if none are listed.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `count` | `size of biome list` | How many biomes from the list below must the player visit. |
| `biomes` | `[]` | A list of biomes the player must visit. Defaults to all biomes in the game. |
| `prefix` | `Fortschritt:` | The prefix that is written before the statistic count. |
| `suffix` | `Biome besucht` | A suffix that should be written after the statistic count in the overview, e.g. `<prefix> 10/100 <suffix>`. |

### money

Tracks the money a player has.

| Config | Default | Description |
| ------ | ------- | ----------- |
| `amount` | `0` | The amount of money the player needs to unlock the achievement. |

## SQL Statements

You can use the following SQL statements to sync the block break data from core protect into the achievement statistics.

```sql
UPDATE `rcachievements_achievements` as a
RIGHT JOIN rcachievements_player_achievements as pa
    ON pa.achievement_id = a.id
LEFT JOIN rcachievements_datastore as data 
    ON data.id = pa.data_id
JOIN rcachievements_players as p 
    ON p.id = pa.player_id
SET data.data = JSON_REPLACE(data.data, '$.count', (
    SELECT SUM(bb.block_count)
    FROM `total_broken_blocks` bb
    WHERE
    	bb.uuid = p.id
    AND JSON_CONTAINS(a.config, CONCAT('"', bb.material, '"'), '$."with.blocks"')
))
WHERE a.type = 'block'
AND pa.when_created > '2021-03-18';

SELECT data.id, alias, p.name, json_extract(data.data, '$.count') as `count`
FROM `rcachievements_achievements` as a
JOIN rcachievements_player_achievements as pa
    ON pa.achievement_id = a.id
JOIN rcachievements_datastore as data ON data.id = pa.data_id
JOIN rcachievements_players as p ON p.id = pa.player_id
WHERE a.type = 'block'
AND data.data != '{}'
AND a.parent_id is NULL
AND pa.when_created > '2021-03-18';
ORDER BY `count` DESC;
```
