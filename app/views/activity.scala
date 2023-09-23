package views.html

import controllers.routes

import lila.activity.activities.*
import lila.activity.model.*
import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User
import lila.swiss.Swiss

object activity:

  def apply(u: User, as: Iterable[lila.activity.ActivityView])(using Context) =
    div(cls := "activity")(
      views.html.game.importGame.formOnly(env.importer.forms.importForm),
    )

  private def subCount(count: Int) = if (count >= maxSubEntries) s"$count+" else s"$count"


  private val entryTag = div(cls := "entry")
  private val subTag   = div(cls := "sub")
  private val scoreTag = tag("score")
  private val winTag   = tag("win")

