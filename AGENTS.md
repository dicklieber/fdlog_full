## Codex Style Rules

- Always put method/function parameters on separate lines in both definitions and calls.

## Build Tool

- This project uses Mill, not sbt.
- Never run `sbt` commands in this repository.
- Use `./mill -i __.compile` for compile validation.
- Use `./mill __.test` for test validation unless a task asks for a narrower test scope.
