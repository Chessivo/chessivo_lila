package lila.app
package mashup

import lila.common.paginator.Paginator
import lila.db.dsl.*
import lila.game.{ Game, Query }
import lila.user.User

import play.api.mvc.Request
import cats.data.NonEmptyList
import play.api.data.FormBinding
import play.api.i18n.Lang

enum GameFilter:
  val name = lila.common.String.lcfirst(toString)
  case All, Me, Rated, Win, Loss, Draw, Playing, Bookmark, Imported, Search

case class GameFilterMenu(
    all: NonEmptyList[GameFilter],
    current: GameFilter
):
  def list = all.toList

object GameFilterMenu:

  import GameFilter.*

  val all: NonEmptyList[GameFilter] =
    NonEmptyList.of(All, Me, Rated, Win, Loss, Draw, Playing, Bookmark, Imported, Search)

  def apply(user: User, nbs: UserInfo.NbGames, currentName: String, isAuth: Boolean): GameFilterMenu =

    val filters: NonEmptyList[GameFilter] = NonEmptyList(
      All,
      List(
      ).flatten
    )

    val current = currentOf(filters, currentName)

    new GameFilterMenu(filters, current)

  def currentOf(filters: NonEmptyList[GameFilter], name: String) =
    filters.find(_.name == name) | filters.head

  private def cachedNbOf(
      user: User,
      nbs: Option[UserInfo.NbGames],
      filter: GameFilter
  ): Option[Int] =
    filter match
      case Bookmark => nbs.map(_.imported)
      case Imported => nbs.map(_.imported)
      case All      => nbs.map(_.imported)
      case Me       => nbs.map(_.imported)
      case Rated    => nbs.map(_.imported)
      case Win      => nbs.map(_.imported)
      case Loss     => nbs.map(_.imported)
      case Draw     => nbs.map(_.imported)
      case Search   => nbs.map(_.imported)
      case Playing  => nbs.map(_.imported)

  final class PaginatorBuilder(
      userGameSearch: lila.gameSearch.UserGameSearch,
      pagBuilder: lila.game.PaginatorBuilder,
      gameRepo: lila.game.GameRepo,
      gameProxyRepo: lila.round.GameProxyRepo,
      bookmarkApi: lila.bookmark.BookmarkApi
  )(using Executor):

    def apply(
        user: User,
        nbs: Option[UserInfo.NbGames],
        filter: GameFilter,
        me: Option[User],
        page: Int
    )(using req: Request[?], formBinding: FormBinding, lang: Lang): Fu[Paginator[Game]] =
      val nb               = cachedNbOf(user, nbs, filter)
      def std(query: Bdoc) = pagBuilder.recentlyCreated(query, nb)(page)
      filter match
        case Bookmark =>
          pagBuilder(
            selector = Query imported user.id,
            sort = $sort desc "pgni.ca",
            nb = nb
          )(page)
        case Imported =>
          pagBuilder(
            selector = Query imported user.id,
            sort = $sort desc "pgni.ca",
            nb = nb
          )(page)
        case All =>
              pagBuilder(
                selector = Query imported user.id,
                sort = $sort desc "pgni.ca",
                nb = nb
              )(page)
        case Me    => std(Query.opponents(user, me | user))
        case Rated => std(Query rated user.id)
        case Win   => std(Query win user.id)
        case Loss  => std(Query loss user.id)
        case Draw  => std(Query draw user.id)
        case Playing =>
          pagBuilder(
            selector = Query nowPlaying user.id,
            sort = $empty,
            nb = nb
          )(page)
            .flatMap {
              _.mapFutureResults(gameProxyRepo.upgradeIfPresent)
            }
            .addEffect { p =>
              p.currentPageResults.filter(_.finishedOrAborted) foreach gameRepo.unsetPlayingUids
            }
        case Search => userGameSearch(user, page)

  def searchForm(
      userGameSearch: lila.gameSearch.UserGameSearch,
      filter: GameFilter
  )(using req: Request[?], formBinding: FormBinding, lang: Lang): play.api.data.Form[?] =
    filter match
      case Search => userGameSearch.requestForm
      case _      => userGameSearch.defaultForm
