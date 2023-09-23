package views.html.user.show

import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes

import lila.api.Context
import lila.app.mashup.UserInfo
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.richText
import lila.user.User

object header:

  private val dataToints = attr("data-toints")
  private val dataTab    = attr("data-tab")

  def apply(u: User, info: UserInfo, angle: UserInfo.Angle, social: UserInfo.Social)(using ctx: Context) =
    frag(
      !ctx.is(u) option noteZone(u, social.notes),
      isGranted(_.UserModView) option div(cls := "mod-zone mod-zone-full none"),
      standardFlash,
      cssTag("importer"),
      div(cls := "angles number-menu number-menu--tabs menu-box-pop")(
        a(
          dataTab := "activity",
          cls := List(
            "nm-item to-activity" -> true,
            "active"              -> (angle == UserInfo.Angle.Activity)
          ),
          href := routes.User.show(u.username)
        )("Analyze New Games"),
        a(
          dataTab := "games",
          cls := List(
            "nm-item to-games" -> true,
            "active"           -> (angle.key == "games")
          ),
          href := routes.User.gamesAll(u.username)
        )(
          "My Analyzed Games"
        )
      ),
      jsModule("user"),
      jsTag("importer.js"),
    )

  def noteZone(u: User, notes: List[lila.user.Note])(implicit ctx: Context) = div(cls := "note-zone")(
    postForm(cls := "note-form", action := routes.User.writeNote(u.username))(
      form3.textarea(lila.user.UserForm.note("text"))(
        placeholder := trans.writeAPrivateNoteAboutThisUser.txt()
      ),
      if (isGranted(_.ModNote))
        div(cls := "mod-note")(
          submitButton(cls := "button", name := "noteType", value := "mod")("Save Mod Note"),
          isGranted(_.Admin) option submitButton(cls := "button", name := "noteType", value := "dox")(
            "Save Dox Note"
          ),
          submitButton(cls := "button", name := "noteType", value := "normal")("Save Regular Note")
        )
      else submitButton(cls := "button", name := "noteType", value := "normal")(trans.save())
    ),
    notes.isEmpty option div(trans.noNoteYet()),
    notes.map { note =>
      div(cls := "note")(
        p(cls := "note__text")(richText(note.text, expandImg = false)),
        (note.mod && isGranted(_.Admin)) option postForm(
          action := routes.User.setDoxNote(note._id, !note.dox)
        )(
          submitButton(
            cls := "button-empty confirm button text"
          )("Toggle Dox")
        ),
        p(cls := "note__meta")(
          userIdLink(note.from.some),
          br,
          note.dox option "dox ",
          if (isGranted(_.ModNote)) momentFromNowServer(note.date)
          else momentFromNow(note.date),
          (ctx.me.exists(note.isFrom) && !note.mod) option frag(
            br,
            postForm(action := routes.User.deleteNote(note._id))(
              submitButton(
                cls      := "button-empty button-red confirm button text",
                style    := "float:right",
                dataIcon := licon.Trash
              )(trans.delete())
            )
          )
        )
      )
    }
  )
