package views.html
package game

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import chess.format.pgn.PgnStr
import lila.fishnet.FishnetLimiter
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import controllers.routes

object importGame:

  private def analyseHelp(using ctx: Context) =
    ctx.isAnon option a(cls := "blue", href := routes.Auth.signup)(trans.youNeedAnAccountToDoThat())
  def getUserTier(ctx: Context): Int = {
    ctx.me match {
      case Some(user) => user.tokenLimit()
      case None => 0
    }
  }
  def formOnly(form: play.api.data.Form[?])(using ctx: Context
  ) = {
    val countFuture = ctx.userId.map { userId =>
      env.fishnet.limiter.getDailyCount(userId)
    }.getOrElse(Future.successful(0))
    val count = Await.result(countFuture, 5.seconds) // Blocks until the Future completes
    val limit = getUserTier(ctx)
    div(id := "main-wrap", cls := "is2d")(
      main(cls := "importer page-small box-pad")(
        h1(cls := "box__top")(trans.importGame()),
        p(cls := "explanation")(trans.importGameExplanation()),
        p(
          a(cls := "text", dataIcon := licon.InfoCircle, href := "https://chessivo.com/how-to-export-your-pgn-files-for-analysis-with-chessivo/")(
            "Please check out our blog post if you need help with importing your games."
          )
        ),
        p(
          a(cls := "text", dataIcon := licon.InfoCircle)(
            ctx.userId.map { userId =>
              val remaining = math.max(0, limit - count)
              s"Remaining games to analyze: $remaining."
            }.getOrElse("Not logged in or invalid UserId")
          )
        ),
        standardFlash,
        postForm(cls := "form3 import", action := routes.Importer.sendGame)(
          form3.group(form("pgn"), trans.pasteThePgnStringHere())(form3.textarea(_)()),
          form("pgn").value flatMap { pgn =>
            lila.importer
              .ImportData(PgnStr(pgn), none)
              .preprocess(none)
              .fold(
                err =>
                  frag(
                    pre(cls := "error")(err),
                    br,
                    br
                  ).some,
                _ => none
              )
          },
          form3.group(form("pgnFile"), trans.orUploadPgnFile(), klass = "upload") { f =>
            form3.file.pgn(f.name)
          },
          input(tpe := "hidden", name := "analyse", value := "true"),
          div(cls := "mode-choice buttons")(
            div(cls := "radio")(
              div(
                input(id := "sf_mode_white", cls := "checked_true", name := "analyzeFor", tpe := "radio", value := "white", checked := ""),
                label(`for` := "sf_mode_white")("Analyze for White")
              ),
              div(
                input(id := "sf_mode_black", cls := "checked_false", name := "analyzeFor", tpe := "radio", value := "black"),
                label(`for` := "sf_mode_black")("Analyze for Black")
              )
            )
          ),
          if (count >= limit) {
            div(cls := "is2d")(
              p(),
              p(cls := "explanation")("Sorry, your daily tokens are used up, come back tomorrow."),
            )
          } else {
            form3.action(form3.submit(trans.importGame(), licon.UploadCloud.some))
          }
        )
      )
    )
  }

  def apply(form: play.api.data.Form[?])(using ctx: Context) =
    val countFuture = ctx.userId.map { userId =>
      env.fishnet.limiter.getDailyCount(userId)
    }.getOrElse(Future.successful(0))
    val count = Await.result(countFuture, 5.seconds) // Blocks until the Future completes
    val limit = getUserTier(ctx)
    views.html.base.layout(
      title = trans.importGame.txt(),
      moreCss = cssTag("importer"),
      moreJs = jsTag("importer.js"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Paste PGN chess game",
          url = s"$netBaseUrl${routes.Importer.importGame.url}",
          description = trans.importGameExplanation.txt()
        )
        .some
    ) {
      main(cls := "importer page-small box box-pad")(
        h1(cls := "box__top")(trans.importGame()),
        p(cls := "explanation")(trans.importGameExplanation()),
        p(
          a(cls := "text", dataIcon := licon.InfoCircle, href := "https://chessivo.com/how-to-export-your-pgn-files-for-analysis-with-chessivo/")(
            "Please check out our blog post if you need help with importing your games."
          )
        ),
        p(
          a(cls := "text", dataIcon := licon.InfoCircle, href := routes.Study.allDefault(1))(
            ctx.userId.map { userId =>
              val remaining = math.max(0, limit - count)
              s"Remaining games to analyze: $remaining."
            }.getOrElse("Not logged in or invalid UserId")
          )
        ),
        standardFlash,
        postForm(cls := "form3 import", action := routes.Importer.sendGame)(
          form3.group(form("pgn"), trans.pasteThePgnStringHere())(form3.textarea(_)()),
          form("pgn").value flatMap { pgn =>
            lila.importer
              .ImportData(PgnStr(pgn), none)
              .preprocess(none)
              .fold(
                err =>
                  frag(
                    pre(cls := "error")(err),
                    br,
                    br
                  ).some,
                _ => none
              )
          },
          form3.group(form("pgnFile"), trans.orUploadPgnFile(), klass = "upload") { f =>
            form3.file.pgn(f.name)
          },
          input(tpe := "hidden", name := "analyse", value := "true"),
          div(cls := "mode-choice buttons")(
            div(cls := "radio")(
              div(
                input(id := "sf_mode_white", cls := "checked_true", name := "analyzeFor", tpe := "radio", value := "white", checked := ""),
                label(`for` := "sf_mode_white")("Analyze for White")
              ),
              div(
                input(id := "sf_mode_black", cls := "checked_false", name := "analyzeFor", tpe := "radio", value := "black"),
                label(`for` := "sf_mode_black")("Analyze for Black")
              )
            )
          ),
          if (count >= limit) {
            div(cls := "is2d")(
              p(),
              p(cls := "explanation")("Sorry, your daily tokens are used up, come back tomorrow."),
            )
          } else {
            form3.action(form3.submit(trans.importGame(), licon.UploadCloud.some))
          }
        )
      )
    }
