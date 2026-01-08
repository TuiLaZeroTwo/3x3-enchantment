# ⚡ Hisa-ECM (Enhanced Custom Miner)

![Version](https://img.shields.io/badge/version-0.0.1-blue) ![API](https://img.shields.io/badge/Folia-1.20.1-green) ![Java](https://img.shields.io/badge/Java-17%2B-orange)

**Hisa-ECM** is a high-performance custom enchantment plugin optimized for **Folia** and Paper. It introduces a GUI-based enchantment system, featuring a powerful 3x3 mining mechanic with built-in anti-dupe and durability synchronization.

## ✨ Features

- **🚀 Folia Support:** Fully threaded and optimized for regionized multithreading.
- **⛏️ 3x3 Mining:** Automatically mines a 3x3 area based on the block face you hit.
- **🛡️ Anti-Dupe:** - Durability is calculated per block broken.
  - Checks if the user actually holds the tool.
  - Prevents "Free Mining" (mining without durability loss).
- **🛑 Sneak-to-Precision:** Hold `Shift` (Sneak) to mine only one block.
- **🖥️ GUI System:** Clean, simple interface to apply enchants.

## 🛠️ Installation

1. Stop your server.
2. Place `Hisa-ECM-0.0.1.jar` into your `plugins` folder.
3. Start the server.

## 🎮 Commands

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/hisaecm` | Opens the Custom Enchant GUI | `None (Everyone)` |

## ⚙️ Usage

1. Hold a **Pickaxe** in your main hand.
2. Type `/hisaecm`.
3. Click the **Nether Star** in the middle of the GUI.
4. Your pickaxe is now enchanted with **3x3 Mining I**.
5. Go mine!

## 🔧 Building from Source

```bash
git clone [https://github.com/yourname/hisa-ecm.git](https://github.com/yourname/hisa-ecm.git)
cd hisa-ecm
./gradlew build