# Noctua Client for Fabric (Minecraft 26.2 / 26.3)

All 55 Noctua mods for current Minecraft. Needs only Fabric Loader 0.17+ and Java 25; works alongside Sodium.
Press **Right Shift** in game to open the mod menu.

26.2 and 26.3 differ internally (rendering, input codes), so there is one jar per version:

    gradle build -Pminecraft_version=26.2
    gradle build -Pminecraft_version=26.3

(Gradle 9.8+, JDK 25.) Shared code is in `src/main`; the few classes that differ are in `src/v262` and `src/v263`.

Settings: `config/lumen/config.json`, profiles in `config/lumen/profiles/`, custom cape image `config/lumen/cape.png`.
The settings file is shared with the 1.8.9 Forge version: each version only rewrites what it knows and keeps the rest.
The Poppins font is licensed under the SIL Open Font License (see `Poppins-OFL.txt`).
