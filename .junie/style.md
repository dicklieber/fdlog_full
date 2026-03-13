### Scala 3 Style Guidelines for Junie

- **Avoid unneeded curly braces `{}`**. Prefer the Scala 3 indentation-based (braceless) syntax.
- Use `:` to start a template body (class, trait, object, enum, or `new` with body).
- For methods with a single expression, use `def method: Type = expression`.
- For multi-line method bodies, use `def method: Type =` followed by an indented block.
- Use `if ... then ... else ...` without parentheses for the condition where appropriate.
- Use `while ... do ...` without parentheses for the condition.
- Use `match` and `case` without braces around the `match` body.
- For `try-catch`, use `try` followed by an indented block and `catch` followed by an indented `case` block.
- Use `for` expressions with `yield` or `do` using indentation.
- When creating anonymous classes or extending classes with a body, use `new ClassName(...):` followed by indented members.
- **Do not collapse text in labels or other text output** (e.g., do not use "..." or other truncation in labels produced by Junie). Ensure all text is fully visible.
