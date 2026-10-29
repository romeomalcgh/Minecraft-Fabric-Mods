# Hostile Spawn Overlay

A client-side Fabric mod for Minecraft Java Edition 26.3 that draws crisp colored outlines on blocks to help identify likely hostile-mob spawning locations.

## Features

- Toggle the overlay with **O**.
- Uses block light and sky light in its spawn-suitability estimate.
- Draws stable world-space outlines instead of fading particles.
- Scans around the player, including a vertical range of four blocks above and below the player's current block Y-level.
- Configure scan radius and update interval using client commands.

## Color legend

- **Red:** estimated spawnable both during day and night.
- **Yellow:** estimated spawnproof during day but spawnable at night.
- **Green:** estimated spawnproof, or the block is not considered a valid floor for the tested hostile mobs.

## Commands

Run these commands in Minecraft chat:

| Command | Description |
| --- | --- |
| `/hso radius <blocks>` | Set scan radius from 1 to 24 blocks. Default: 8. |
| `/hso interval <ticks>` | Set refresh interval from 1 to 200 ticks. Default: 4 ticks (0.2 seconds). |
| `/hso status` | Show whether the overlay is enabled and its current settings. |
| `/hso reset` | Restore the default radius and refresh interval. |

The overlay itself remains toggled with **O**. Command settings currently apply for the current game session and reset when Minecraft is restarted.

Larger radii and faster refresh intervals increase the amount of work performed on the client. If performance drops, lower the radius or increase the interval.

## Requirements

- Minecraft Java Edition 26.3
- Fabric Loader 0.20.1 or compatible
- Fabric API 0.165.0+26.3
- Java 25 or newer

## Installation

1. Install Fabric for Minecraft 26.3.
2. Install Fabric API for the same Minecraft version.
3. Put `HostileSpawnOverlay-1.1.0+mc26.3.jar` in the instance's `mods` folder.
4. Launch the game and press **O** to toggle the overlay.

## Build from source

This project uses Fabric Loom and Gradle. With Java 25 installed, run:

```powershell
.\gradlew.bat build
```

If you do not have a Gradle wrapper in your checkout, run the build using a compatible Gradle 9.7.1 installation.

The compiled mod JAR and sources JAR are generated in `build/libs/`.

## Important accuracy note

This overlay is a visual estimate, not a guarantee of actual spawn eligibility. It checks a subset of floor, collision, fluid, block-light, and sky-light conditions for several common hostile mobs. Vanilla spawn rules and modded entities can have additional requirements, so the colors may not match every possible spawn situation.

## AI-assisted development disclosure

Parts of this project's source code were written with assistance from AI tools. The code was also directed, integrated, and modified as part of the project workflow. AI assistance may have introduced mistakes; review and test the code before relying on it.

## License

No license has been specified yet. Unless a license is added to this repository, reuse and redistribution permissions are not explicitly granted.
