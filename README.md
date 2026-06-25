# Zi's Music Player (ZMP)

A sleek, modern music player for Minecraft built using Fabric and ImGui. Take control of Minecraft's soundtrack with an elegant, fully-featured in-game music player and "Now Playing" HUD overlay!

## Features

- **Modern UI**: An intuitive ImGui-based interface to manage your music.
- **Library Management**: Browse through Ambient tracks, Music Discs, and create Custom Playlists.
- **Playback Controls**: Play, pause, skip, shuffle, and set tracks to repeat.
- **HUD Overlay**: A beautiful "Now Playing" overlay with track name, artist, and live playback progress.
- **Toast Notifications**: Modern toast alerts pop up dynamically whenever a new track starts.

## Controls

- Press **M** to open/close the Music Player interface (can be changed in Keybinds).
- Click the star icon next to any song to add or remove it from your Playlist.
- Toggle the HUD, repeat modes, and vanilla delay logic in the "Settings" tab.

## Setup & Building

This mod requires **Fabric Loader** and **Fabric API**.

1. Clone the repository.
2. Open the project in your IDE of choice (IntelliJ IDEA or Eclipse recommended).
3. Run `gradlew genSources` to map the source code.
4. Run `gradlew build` to compile the mod into the `build/libs` directory.

## License

This project is licensed under the MIT License. Feel free to use, modify, and distribute the code!
