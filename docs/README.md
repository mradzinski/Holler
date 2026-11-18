# How Holler works inside

Five diagrams, meant to be read in this order. Each `.puml` file is the source of the image next to it.

| # | Diagram | Read it to learn |
|---|---|---|
| 1 | [Overview](holler-1-overview.png) | Who does what when a message is shown: your app, the public API and the internals, in 13 numbered steps. |
| 2 | [Message lifecycle](holler-2-message-lifecycle.png) | What happens over time: show, draw, close and replace. |
| 3 | [States](holler-3-states.png) | The two state machines inside a message: its place in the queue and its countdown. |
| 4 | [Public API](holler-4-public-api.png) | Everything an app can call and pass, grouped by purpose. |
| 5 | [Internals](holler-5-internals.png) | The classes behind the public API and how they hold each other. |

## Render them again

The diagrams are written by hand, so they are a snapshot: update the `.puml` file when the code changes, then
render it with [PlantUML](https://plantuml.com) (which needs Graphviz):

```bash
plantuml -tpng -tsvg docs/*.puml
```
