# Changelog

What changed in each version of Holler, newest first. Versions follow
[semantic versioning](https://semver.org).

## 1.0.0-rc02 (not released yet)

### Changed

- **Pile: card heights move with the pile.** When another card becomes the front one, the cards behind
  it move to its height over the same 200 ms the pile takes to move, where they used to change size in
  one step. A card that comes forward grows or shrinks to its own height the same way.

### Fixed

- **Pile: a message that goes under the pile and comes back keeps its state.** A message beyond
  `maxVisible` is not drawn, and it used to come back as a new card: collapsed, with its timer started
  over, and with a timer the user had stopped by expanding it running again. It now comes back with the
  time it had left, still expanded, and with a stopped timer still stopped. The expand button's hint
  movement is not repeated either.
- **Pile: a card shorter than the front one takes its height.** It used to keep its own height, so for
  a host at the top of the screen it did not show past a taller front card.
- **A message replaced while it enters no longer enters twice.** When a message was replaced during its
  enter animation by a value drawn from another place in the app's code, such as another branch of a
  `when`, the enter animation started again from the beginning. The new card now shows at once.

## 1.0.0-rc01 (2026-11-18)

Initial release.
