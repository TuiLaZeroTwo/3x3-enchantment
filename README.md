# H I S A - E C M

> Advanced Economy, Custom Enchants, and Server Mechanics.
> Optimized for Folia & Paper 1.20+

---

## 01 // OVERVIEW

Hisa-ECM is a modular core plugin designed for modern Minecraft survival and economy servers. It replaces multiple standalone plugins by integrating custom enchantments, economy utilities, and automation machines into a single, high-performance package.

Built with Folia multi-threading support in mind.

## 02 // FEATURES

### Core Mechanics
* **Chunk Loader Bots** :: Physical Armor Stands that keep chunks loaded. Fuel them with Coal Blocks.
* **Crop Boosters** :: Placeable crystals that accelerate crop growth in a radius.
* **Auto-Clear System** :: Lag reduction task that clears ground items on a configurable interval.

### Custom Enchantments
* **Vein Miner** :: Breaks entire ore veins or trees instantly. (Sneak to activate).
* **Explosive** :: Chance to blast a 3x3 area when mining.
* **3x3 Mining** :: Mines a 3x3 area suitable for tunnel boring.
* **Telekinesis** :: Items go directly to inventory.
* **Auto-Smelt** :: Automatically smelts ores into ingots.
* **Haste Aura** :: Passive mining speed boost.

### Economy Modules
* **Bounty System** :: Place, track, and claim shard-based bounties on other players.
* **Shop System** :: GUI-based shop for buying custom items and enchants.
* **Withdraw** :: (Planned) Convert currency into physical notes.

## 03 // COMMANDS

| Command | Description | Permission |
| :--- | :--- | :--- |
| /hisaecm help | Show the help menu | hisaecm.help |
| /hisaecm reload | Reload configuration files | hisaecm.admin |
| /hisaecm give <item> | Give special items (Loaders, Boosters) | hisaecm.admin |
| /hisaecm enchant | Open the Enchantment Shop | hisaecm.enchant |
| /bounty | Open the Bounty Menu | hisaecm.bounty |

## 04 // CONFIGURATION

The plugin generates the following configuration files:

* config.yml :: Main settings, message customization, and enchantment probabilities.
* bounties.yml :: Database for active player bounties.
* loaders.yml :: Data storage for active Chunk Loaders.
* boosters.yml :: Data storage for active Crop Boosters.

## 05 // INSTALLATION

1.  Stop your server.
2.  Drop Hisa-ECM.jar into the /plugins folder.
3.  Ensure PlaceholderAPI is installed (Required for economy).
4.  Start the server.
5.  Configure settings in config.yml.

## 06 // REQUIREMENTS

* Java 17 or higher
* Paper, Purpur, or Folia (1.20.1+)
* PlaceholderAPI (for Shards economy hook)

---

[ Source Code ]  [ Report Issues ]  [ Wiki ]