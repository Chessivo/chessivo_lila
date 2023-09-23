package views.html.account

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object layout:

  def apply(
      title: String,
      active: String,
      evenMoreCss: Frag = emptyFrag,
      evenMoreJs: Frag = emptyFrag
  )(body: Frag)(implicit ctx: Context): Frag =
    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("account"), evenMoreCss),
      moreJs = frag(jsModule("account"), evenMoreJs)
    ) {
      def activeCls(c: String) = cls := active.activeO(c)
      main(cls := "account page-menu")(
        ctx.me.exists(_.enabled.yes) option st.nav(cls := "page-menu__menu subnav")(
          lila.pref.PrefCateg.values.map { categ =>
            a(activeCls(categ.slug), href := routes.Pref.form(categ.slug))(
              bits.categName(categ)
            )
          },
        ),
        div(cls := "page-menu__content")(body)
      )
    }
