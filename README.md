# Hidden Object Examiner

A RuneLite plugin for discovering unnamed scenery that the game normally does not place in the right-click menu.

- Highlights rendered, unnamed objects with no normal actions.
- Adds an `Examine` entry while the mouse is over one of those objects.
- Labels the entry with the object ID, for example `Unnamed object (688)`.
- Uses the game's normal object-examine action, so the examine sentence comes from Jagex rather than a bundled text database.

The highlight, color, maximum distance, and restored menu entry are configurable. The plugin does not add gameplay interactions or automate input.

## Development

Build and test with Java 11:

```text
./gradlew clean test
./gradlew run
```
