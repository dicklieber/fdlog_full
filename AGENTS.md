## Codex Style Rules


## Build Tool

- This project uses Mill, not sbt.
- Never run `sbt` commands in this repository.
- Use `./mill -i __.compile` for compile validation.
- Use `./mill __.test` for test validation unless a task asks for a narrower test scope.
