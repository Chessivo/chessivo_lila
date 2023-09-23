package views.html.analyse

import bits.dataPanel
import chess.format.Fen
import chess.variant.Crazyhouse
import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.game.Pov
import lila.rating.PerfType.iconByVariant

object replay:

  private[analyse] def titleOf(pov: Pov)(implicit lang: Lang) =
    s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}: ${pov.game.opening
        .fold(trans.analysis.txt())(_.opening.name)}"

  def apply(
      pov: Pov,
      data: play.api.libs.json.JsObject,
      initialFen: Option[chess.format.Fen.Epd],
      pgn: String,
      analysis: Option[lila.analyse.Analysis],
      analysisStarted: Boolean,
      simul: Option[lila.simul.Simul],
      cross: Option[lila.game.Crosstable.WithMatchup],
      userTv: Option[lila.user.User],
      chatOption: Option[lila.chat.UserChat.Mine],
      bookmarked: Boolean
  )(implicit ctx: Context) =

    import pov.*

    val imageLinks = frag(
      a(
        dataIcon := licon.NodeBranching,
        cls      := "text game-gif",
        targetBlank,
        href := cdnUrl(
          routes.Export.gif(pov.gameId, pov.color.name, ctx.pref.theme.some, ctx.pref.pieceSet.some).url
        )
      )(trans.gameAsGIF()),
      a(
        dataIcon := licon.NodeBranching,
        cls      := "text position-gif",
        targetBlank,
        href := cdnUrl(
          routes.Export
            .fenThumbnail(
              Fen.write(pov.game.situation).value,
              pov.color.name,
              None,
              pov.game.variant.key.some,
              ctx.pref.theme.some,
              ctx.pref.pieceSet.some
            )
            .url
        )
      )(trans.screenshotCurrentPosition())
    )
    val shareLinks = frag(
      a(dataIcon := licon.Expand, cls := "text embed-howto")(trans.embedInYourWebsite()),
      div(
        input(
          id         := "game-url",
          cls        := "copyable autoselect",
          spellcheck := "false",
          readonly,
          value := s"${netBaseUrl}${routes.Round.watcher(pov.gameId, pov.color.name)}"
        ),
        button(
          title    := "Copy URL",
          cls      := "copy button",
          dataRel  := "game-url",
          dataIcon := licon.Link
        )
      )
    )
    val pgnLinks = frag(
      a(
        dataIcon := licon.Download,
        cls      := "text",
        href     := s"${routes.Game.exportOne(game.id)}?literate=1",
        downloadAttr
      )(
        trans.downloadAnnotated()
      ),
      a(
        dataIcon := licon.Download,
        cls      := "text",
        href     := s"${routes.Game.exportOne(game.id)}?evals=0&clocks=0",
        downloadAttr
      )(
        trans.downloadRaw()
      ),
      game.isPgnImport option a(
        dataIcon := licon.Download,
        cls      := "text",
        href     := s"${routes.Game.exportOne(game.id)}?imported=1",
        downloadAttr
      )(trans.downloadImported())
    )

    // Step 1: Import PGN
    val pgnImportResult = lila.study.PgnImport(chess.format.pgn.PgnStr(pgn), Nil).toOption

    // Step 2 and 3: Traverse the mainline and build the mapping
    val fenMap = scala.collection.mutable.Map[String, Int]()
    pgnImportResult.foreach { result =>
      def traverseMainline(node: lila.tree.Node, ply: Int): Unit = {
        fenMap(node.fen.value) = ply
        // Only traverse the mainline (first child in each branch)
        node.children.first.foreach { child =>
          traverseMainline(child, ply + 1)
        }
      }

      traverseMainline(result.root, 1)
    }

    // Step 4: Serialize the mapping to JSON

    import play.api.libs.json.Json

    val fenJson = Json.toJson(fenMap.toMap)

    bits.layout(
      title = titleOf(pov),
      moreCss = frag(
        cssTag("analyse.free"),
        cssTag("analyse.round"),
        pov.game.variant == Crazyhouse option cssTag("analyse.zh"),
        ctx.blind option cssTag("round.nvui")
      ),
      moreJs = frag(
        analyseTag,
        analyseNvuiTag,
        embedJsUnsafeLoadThen(
          s"""LichessAnalyse.boot(${
            safeJsonValue(
              Json
                .obj(
                  "data" -> data,
                  "i18n" -> jsI18n(),
                  "analyse--wiki" -> true,
                  "wiki" -> true,
                  "userId" -> ctx.userId
                )
                .add("hunter" -> isGranted(_.ViewBlurs)) ++
                views.html.board.bits.explorerAndCevalConfig
            )
          })"""),
        embedJsUnsafe(
          analysis.map(an => "lichess.payload = '" + an.get_payload + "';").getOrElse("")
             ),
        embedJsUnsafe("lichess.fenmap = JSON.parse('" + fenJson.toString + "');"),
      ),
      openGraph = povOpenGraph(pov).some
    )(
      frag(
        main(cls := List(
          "analyse" -> true,
          "analyse--wiki" -> pov.game.variant.standard
        ))(
          st.aside(cls := "analyse__side")(
            views.html.game
              .side(
                pov,
                initialFen,
                none,
                simul = simul,
                none,
                bookmarked = bookmarked
              ),
            views.html.base.bits.mselect(
              "analyse-variant",
              span(cls := "text", dataIcon := iconByVariant(pov.game.variant))(pov.game.variant.name),
              List(pov.game.variant).map { v =>
                a(
                  dataIcon := iconByVariant(v),
                  cls := "current"
                )(v.name)
              }
            ),
            pov.game.variant.standard option div(cls := "analyse__wiki")
          ),
          div(cls := "analyse__board main-board")(chessgroundBoard),
          div(cls := "analyse__tools")(div(cls := "ceval")),
          div(cls := "analyse__controls"),
          !ctx.blind option frag(
            div(cls := "analyse__underboard")(
              div(role := "tablist", cls := "analyse__underboard__menu")(
                game.analysable option
                  span(role := "tab", cls := "computer-analysis", dataPanel := "computer-analysis")(
                    trans.computerAnalysis()
                  ),
                span(role := "tab", dataPanel := "fen-pgn")(trans.study.shareAndExport())
              ),
              div(cls := "analyse__underboard__panels")(
                game.analysable option div(cls := "computer-analysis")(
                  if (analysis.isDefined || analysisStarted) div(id := "acpl-chart")
                  else
                    postForm(
                      cls    := s"future-game-analysis${ctx.isAnon ?? " must-login"}",
                      action := routes.Analyse.requestAnalysis(gameId)
                    )(
                      submitButton(cls := "button text")(
                        span(cls := "is3 text", dataIcon := licon.BarChart)("Request Chessivo AI analysis")
                      )
                    )
                ),
                div(cls := "fen-pgn")(
                  div(
                    strong("FEN"),
                    input(
                      readonly,
                      spellcheck := false,
                      cls        := "copyable autoselect like-text analyse__underboard__fen"
                    )
                  ),
                  div(
                    strong("PGN"),
                    pgnLinks
                  ),
                  div(cls := "pgn")(pgn)
                )
              )
            )
          )
        ),
        ctx.blind option div(cls := "blind-content none")(
          h2("PGN downloads"),
          pgnLinks,
          button(cls := "copy-pgn", attr("data-pgn") := pgn)(
            "Copy PGN to clipboard"
          )
        )
      )
    )
