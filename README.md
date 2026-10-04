# Hidden Object Examiner

A RuneLite plugin for discovering scenery that the game normally does not place in the right-click menu.

- Immediately highlights rendered objects whose scene tags mark them as excluded from normal mouse/menu picking.
- Adds an `Examine` entry while the mouse is over one of those objects.
- Labels the entry with its active object name and ID, for example `Shield display (12345)`, or `Unnamed object (688)` when it has no name.
- Uses the game's normal object-examine action, so the examine sentence comes from Jagex rather than a bundled text database.
- Classifies objects only as scenes load or objects spawn, then retains a tile-indexed cache so rendering and cursor checks never rescan the scene.

The highlight, color, maximum distance, and restored menu entry are configurable. The plugin does not add gameplay interactions or automate input.

## Development

Build and test with Java 11:

```text
./gradlew clean test
./gradlew run
```
