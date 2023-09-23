package views.html.lobby

import controllers.routes
import controllers.report.routes.{Report as reportRoutes}
import play.api.libs.json.Json

import lila.api.Context
import lila.app.mashup.Preload.Homepage
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.LangPath
import lila.common.String.html.safeJsonValue
import lila.game.Pov
import lila.api.{Nonce, AnnounceStore, Context}
import lila.app.templating.Environment.{given, *}
import lila.app.ContentSecurityPolicy
import lila.app.ui.ScalatagsTemplate.{*, given}
import lila.common.base.StringUtils.escapeHtmlRaw
import lila.common.String.html.safeJsonValue
import lila.common.LangPath
import lila.app.ui.ScalatagsExtensions.opaqueIntFrag

object home:

  def apply(homepage: Homepage)(using ctx: Context) =
    import homepage.*
    views.html.base.layout(
      title = "",
      fullTitle = Some {
        s"$siteName • ${trans.freeOnlineChess.txt()}"
      },
      moreJs = frag(
        embedJsUnsafe(
          s"""window.location.href = "${ctx.me.fold("https://chessivo.com/login")(user => routes.User.show(user.username).url)}";"""
      ))
    )(
      div(cls := "homepage")
    )
