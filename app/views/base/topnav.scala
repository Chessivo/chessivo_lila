package views.html.base

import controllers.clas.routes.{ Clas as clasRoutes }
import controllers.routes

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object topnav:

  private def linkTitle(url: String, name: Frag)(using ctx: Context) =
    if (ctx.blind) h3(name) else a(href := url)(name)

  private def canSeeClasMenu(using ctx: Context) =
    ctx.hasClas || ctx.me.exists(u => u.hasTitle || u.roles.contains("ROLE_COACH"))

  def apply()(using ctx: Context) =
  st.nav(id := "topnav", cls := "hover")(

  )
