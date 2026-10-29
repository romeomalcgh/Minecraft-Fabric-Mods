# Armor Stand Block Heads

A small Fabric mod for Minecraft Java Edition 26.3 that lets players place a single block item on an armor stand's head slot and retrieve it with a second right-click.

## Features

- Place one block item on an armor stand's head when its head slot is empty.
- Right-click the stand again to retrieve the placed block.
- Returns the item safely to the inventory when possible, dropping it if the inventory is full.
- The interaction and item changes are handled server-side, so it is designed to work in single-player and on servers that have the mod installed.

## Requirements

- Minecraft Java Edition 26.3
- Fabric Loader 0.19.5 or newer
- Fabric API
- Java 25 or newer

## Installation

1. Install Fabric for Minecraft 26.3.
2. Install Fabric API for the same Minecraft version.
3. Put `ArmorStandBlockHeads-1.0.3+mc26.3.jar` in the instance's `mods` folder.
4. Launch the game.

## Build from source

This project uses Fabric Loom and Gradle. With Java 25 installed, run:

```powershell
.\gradlew.bat build
```

If you do not have a Gradle wrapper in your checkout, run the build using a compatible Gradle 9.7.1 installation.

The compiled mod JAR and sources JAR are generated in `build/libs/`.

## AI-assisted development disclosure

Parts of this project's source code were written with assistance from AI tools. The code was also directed, integrated, and modified as part of the project workflow. AI assistance may have introduced mistakes; review and test the code before relying on it.

## Compatibility note

The mod is built for Minecraft 26.3 and may require updates for other Minecraft or Fabric API versions.

## License

No license has been specified yet. Unless a license is added to this repository, reuse and redistribution permissions are not explicitly granted.
