# Noctua Client

> **Noctua** (formerly Lumen) is a legit PvP client published as mods only:
> `mod/` (Minecraft 1.8.9, Forge), `mod-fabric/` (Minecraft 26.2 / 26.3, Fabric) and ready-built jars in `dist/`.
> Put the jar for your version in the game's `mods` folder; press **Right Shift** in game for the menu.
>
> The launcher in `launcher/` is no longer developed. The text below describes it as it was, under its old name.

A personal, non-commercial launcher and PvP client for **Minecraft: Java Edition**.

- `launcher/` – the desktop launcher, written in Rust (UI with [egui](https://github.com/emilk/egui)).
- `mod/` – the Lumen client mod for Minecraft 1.8.9 (Forge): HUD, cosmetics and quality-of-life features only.

Lumen Client is not affiliated with, endorsed by or approved by Mojang or Microsoft.

## What the launcher does

1. Signs the user in with **their own Microsoft account**.
2. Downloads the official, unmodified game files, libraries, assets and Java runtime from Mojang's servers.
3. Installs a mod loader from its official source (Forge for 1.8.9, Fabric for 26.2 / 26.3).
4. Starts the game.

It manages three separate game folders: 1.8.9 Forge (with the Lumen mod), 26.3 Fabric and 26.2 Fabric (with Sodium, downloaded from Modrinth).

## Sign-in and the Minecraft API

All sign-in code is in [`launcher/src/auth.rs`](launcher/src/auth.rs).

- **Flow:** OAuth 2.0 device code flow against `login.microsoftonline.com/consumers`, scopes `XboxLive.signin offline_access`. There is no client secret and no redirect URI. The launcher never sees or stores the user's password.
- **Requests made:**
  - `user.auth.xboxlive.com/user/authenticate`
  - `xsts.auth.xboxlive.com/xsts/authorize`
  - `api.minecraftservices.com/authentication/login_with_xbox`
  - `api.minecraftservices.com/minecraft/profile`
- **Why:** the access token and profile (name, UUID) are required to start the game for an account that owns it. Nothing else is read or changed on the account.
- **Token storage:** the refresh token is kept in `account.json` inside the launcher's data folder on the user's own PC. It is sent only to Microsoft's token endpoint. The Minecraft access token is kept in memory and passed to the game process on launch.
- **No offline mode:** the game cannot be started without a signed-in account that owns Minecraft: Java Edition.
- **Client ID:** no Azure application ID is built in; it is entered in the launcher's Settings tab.

## What the launcher does not do

- It does not distribute Minecraft, modified game files or OptiFine. OptiFine is only copied from a jar the user already has on their PC.
- It does not collect analytics and has no server of its own. Network requests go only to Microsoft/Xbox/Mojang, the Forge and Fabric Maven repositories, and Modrinth.

## Building

Launcher (Rust 1.80+):

```
cd launcher
cargo build --release
```

Mod (JDK 17+ to run Gradle 8.8, Java 8 toolchain is fetched automatically):

```
cd mod
gradle build
```

The launcher embeds the built mod from `launcher/assets/LumenClient.jar`.

## Third-party

- Poppins font, SIL Open Font License 1.1 (`launcher/assets/Poppins-OFL.txt`).
- SpongePowered Mixin is shaded into the mod jar.
