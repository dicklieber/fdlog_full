package fdswarm.model.sections


import munit.FunSuite
import com.typesafe.config.ConfigFactory

final class SectionsProviderTest extends FunSuite:

  test("SectionsProvider loads US/CA/DX into SectionsConfig"):
    val confStr =
      """
        |fdswarm.sections {
        |  US = [
        |    { code = "IL",  name = "Illinois" }
        |    { code = "EMA", name = "Eastern Massachusetts" }
        |  ]
        |  CA = [
        |    { code = "ON",  name = "Ontario (outside GTA)" }
        |  ]
        |  DX = { code = "DX", name = "Outside US / Canada" }
        |}
        |""".stripMargin

    val config = ConfigFactory.parseString(confStr).resolve()

    val provider = new SectionsProvider(config)
    val sections = provider.sectionsConfig

    assertEquals(
      sections,
      SectionsConfig(
        US = Seq(
          Section("IL", "Illinois"),
          Section("EMA", "Eastern Massachusetts")
        ),
        CA = Seq(
          Section("ON", "Ontario (outside GTA)")
        ),
        DX = Section("DX", "Outside US / Canada")
      )
    )

  test("SectionsConfig.all returns US ++ CA :+ DX"):
    val confStr =
      """
        |fdswarm.sections {
        |  US = [ { code = "IL", name = "Illinois" } ]
        |  CA = [ { code = "ON", name = "Ontario" } ]
        |  DX = { code = "DX", name = "Outside" }
        |}
        |""".stripMargin

    val provider = new SectionsProvider(ConfigFactory.parseString(confStr).resolve())
    val all = provider.sectionsConfig.all

    assertEquals(all.map(_.code), Seq("IL", "ON", "DX"))

  test("SectionsProvider provides a Seq[Section] as a val"):
    val confStr =
      """
        |fdswarm.sections {
        |  US = [ { code = "IL", name = "Illinois" } ]
        |  CA = [ { code = "ON", name = "Ontario" } ]
        |  DX = { code = "DX", name = "Outside" }
        |}
        |""".stripMargin

    val provider = new SectionsProvider(ConfigFactory.parseString(confStr).resolve())
    val sections: Seq[Section] = provider.sections
    assertEquals(sections.map(_.code), Seq("IL", "ON", "DX"))