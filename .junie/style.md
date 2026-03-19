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
  - Always set `minWidth = scalafx.scene.layout.Region.USE_PREF_SIZE` for `Label`, `TextField`, `ComboBox`, and `Spinner`.
  - Set `textOverrun = OverrunStyle.Clip` where applicable to avoid ellipses.
  - Do not use `minWidth = 0` or any other setting that allows these controls to collapse.
  - In CSS, use `-fx-min-width: -fx-pref-width` and `-fx-text-overrun: clip`.
    CRITICAL FORMATTING RULES (MUST FOLLOW):

# Code Generation Rules (STRICT)

## Output Completeness
- NEVER use "..." (ellipsis) in code or data structures
- NEVER omit values, fields, or parameters
- NEVER summarize or truncate code
- ALWAYS produce full, explicit, copy-paste runnable code

## Data Structures
- All fields must have concrete values
- No placeholders
- No elided members

## Failure Condition
- If any output contains "...", the output is INVALID
- Regenerate until complete