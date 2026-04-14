package fdswarm.logging

/** Implement this trait when a value should expand into multiple structured log fields.
 *
 * Example:
 * {{{
 * final case class UserContext(userId: String, role: String) extends LogFields:
 *   def logFields: Seq[(String, Any)] =
 *     Seq(
 *       "user.id" -> userId,
 *       "user.role" -> role
 *     )
 * }}}
 */
trait LogFields:
  def logFields: Seq[(String, Any)]
