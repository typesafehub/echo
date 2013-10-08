/**
 * Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.trace

import play.api.test.{ FakeApplication, FakeRequest, TestServer }
import play.api._
import play.api.mvc._

object TestApplication {
  import play.core._
  import play.core.Router._
  import play.core.j._

  import play.api.mvc._

  import Router.queryString
  import scala.concurrent.ExecutionContext.Implicits.global
  import play.api.libs.iteratee.Enumerator

  object Routes extends Router.Routes {
    private var _prefix = "/"

    def setPrefix(prefix: String) {
      _prefix = prefix
      List[(String, Routes)]().foreach {
        case (p, router) ⇒ router.setPrefix(prefix + (if (prefix.endsWith("/")) "" else "/") + p)
      }
    }

    def prefix = _prefix

    lazy val defaultPrefix = { if (Routes.prefix.endsWith("/")) "" else "/" }
    // Gets
    private[this] lazy val Application_index =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("get"))))
    private[this] lazy val Application_indexSampled =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("getSampled"))))
    private[this] lazy val GetControllers_withSession =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("getWithSession"))))
    private[this] lazy val GetControllers_show =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("get/sync/"),
        DynamicPart("id", """[^/]+"""))))
    private[this] lazy val GetControllers_showFiltered =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("get/filtered/"),
        DynamicPart("id", """[^/]+"""))))
    private[this] lazy val GetControllers_showAsync =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("get/async/"),
        DynamicPart("duration", """[^/]+"""))))
    private[this] lazy val GetControllers_showAsyncImmediate =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("get/async_immediate"))))
    private[this] lazy val GetControllers_showChunked =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("get/chunked/"),
        DynamicPart("id", """[^/]+"""))))
    private[this] lazy val GetControllers_showLarge =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("get/large"))))
    private[this] lazy val GetControllers_error =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("error"))))

    // Posts
    private[this] lazy val PostControllers_post =
      Route("POST", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("post"))))
    private[this] lazy val PostControllers_uploadFile =
      Route("POST", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("uploadFile"))))

    // Puts
    private[this] lazy val PutControllers_put =
      Route("PUT", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("put"))))

    def documentation = Nil // Documentation not needed for tests

    def routes: PartialFunction[RequestHeader, Handler] = {
      case Application_index(params) ⇒ call {
        invokeHandler(controllers.Application.index,
          HandlerDef(this, "controllers.Application", "index", Nil, "GET", """ Home page""", Routes.prefix + """get"""))
      }
      case Application_indexSampled(params) ⇒ call {
        invokeHandler(controllers.Application.indexSampled,
          HandlerDef(this, "controllers.Application", "indexSampled", Nil, "GET", """ Home page sampled""", Routes.prefix + """getSampled"""))
      }
      case GetControllers_withSession(params) ⇒ call {
        invokeHandler(controllers.GetControllers.getWithSession,
          HandlerDef(this, "controllers.GetControllers", "getWithSession", Nil, "GET", "", Routes.prefix + """getWithSession"""))
      }
      case GetControllers_show(params) ⇒ call(params.fromPath[Int]("id", None)) { (id) ⇒
        invokeHandler(controllers.GetControllers.show(id),
          HandlerDef(this, "controllers.GetControllers", "show", Seq(classOf[Int]), "GET", """""", Routes.prefix + """get/sync/$id<[^/]+>"""))
      }
      case GetControllers_showFiltered(params) ⇒ call(params.fromPath[Int]("id", None)) { (id) ⇒
        invokeHandler(controllers.GetControllers.showFiltered(id),
          HandlerDef(this, "controllers.GetControllers", "showFiltered", Seq(classOf[Int]), "GET", """""", Routes.prefix + """get/filtered/$id<[^/]+>"""))
      }
      case GetControllers_showAsync(params) ⇒ call(params.fromPath[Int]("duration", None)) { (duration) ⇒
        invokeHandler(controllers.GetControllers.showAsync(duration),
          HandlerDef(this, "controllers.GetControllers", "showAsync", Seq(classOf[Int]), "GET", """""", Routes.prefix + """get/async/$duration<[^/]+>"""))
      }
      case GetControllers_showAsyncImmediate(params) ⇒ call {
        invokeHandler(controllers.GetControllers.showAsyncImmediate,
          HandlerDef(this, "controllers.GetControllers", "showAsyncImmediate", Nil, "GET", """""", Routes.prefix + """get/async_immediate"""))
      }
      case GetControllers_showChunked(params) ⇒ call(params.fromPath[Int]("id", None)) { (id) ⇒
        invokeHandler(controllers.GetControllers.showChunked(id),
          HandlerDef(this, "controllers.GetControllers", "showChunked", Seq(classOf[Int]), "GET", """""", Routes.prefix + """get/chunked/$id<[^/]+>"""))
      }
      case GetControllers_showLarge(params) ⇒ call {
        invokeHandler(controllers.GetControllers.showLarge,
          HandlerDef(this, "controllers.GetControllers", "showLarge", Nil, "GET", """""", Routes.prefix + """get/large"""))
      }
      case GetControllers_error(params) ⇒ call {
        invokeHandler(controllers.GetControllers.error,
          HandlerDef(this, "controllers.GetControllers", "error", Nil, "GET", """""", Routes.prefix + """error"""))
      }
      case PostControllers_post(params) ⇒ call {
        invokeHandler(controllers.PostControllers.post,
          HandlerDef(this, "controllers.PostControllers", "post", Nil, "POST", """""", Routes.prefix + """post"""))
      }
      case PostControllers_uploadFile(params) ⇒ call {
        invokeHandler(controllers.PostControllers.uploadFile,
          HandlerDef(this, "controllers.PostControllers", "uploadFile", Nil, "POST", """""", Routes.prefix + """uploadFile"""))
      }
      case PutControllers_put(params) ⇒ call {
        invokeHandler(controllers.PutControllers.put,
          HandlerDef(this, "controllers.PutControllers", "put", Nil, "PUT", """""", Routes.prefix + """put"""))
      }
    }
  }

  object controllers {
    import play.api._
    import play.api.mvc._

    object Application extends Controller {
      val index = Action {
        Ok("invoked index")
      }
      val indexSampled = Action {
        Ok("invoked indexSampled")
      }
    }

    object GetControllers extends Controller {
      def show(id: Int) = Action {
        Ok("invoked show with: " + id)
      }

      def showFiltered(id: Int) = Action {
        Ok("invoked showFiltered with: " + id)
      }

      def getWithSession = Action { request ⇒
        request.session.get("key").map { v ⇒
          Ok("Got session value: " + v)
        } getOrElse {
          Ok("Setting session variable: key -> value").withSession("key" -> "value")
        }
      }

      def showAsync(duration: Int) = Action {
        Async(scala.concurrent.Future {
          if (duration > 0) Thread.sleep(duration)
          Ok("invoked showAsync with: " + duration)
        })
      }

      def showAsyncImmediate = Action {
        Async(scala.concurrent.Future(Ok("Async with immediate return")))
      }

      def showChunked(id: Int) = Action {
        Ok.stream(
          Enumerator((1 to 10).map("chunk_" + _): _*).andThen(Enumerator.eof))
      }

      def showLarge = Action {
        SimpleResult(
          header = ResponseHeader(200),
          body = Enumerator(largeData))
      }

      def error = Action {
        throw new Exception("This page generates an error!")
        Ok("This page will generate an error!")
      }
    }

    object PostControllers extends Controller {
      def post = Action {
        Ok("post body")
      }

      // POST && File upload
      def uploadFile = Action(parse.multipartFormData) { request ⇒
        request.body.file("picture.txt").map { picture ⇒
          Ok("File uploaded: " + picture.filename)
        }.getOrElse {
          BadRequest("Not Ok")
        }
      }
    }

    object PutControllers extends Controller {
      def put = Action {
        Ok("put body")
      }
    }
  }

  lazy val application: FakeApplication = {
    val r = FakeApplication(additionalConfiguration = Map("application.router" -> "com.typesafe.trace.TestApplication$Routes",
      "application.secret" -> "tbWgMhvWQ?mIxxjV^VQ6o4@rU2sfChxV`=B63HKJtS>W3OQMe9wNljXUef3^cQ`r",
      "logger.root" -> "OFF",
      "logger.play" -> "OFF",
      "logger.application" -> "OFF"))
    r
  }

  val largeData = """1:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
2:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
3:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
4:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
5:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
6:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
7:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
8:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
9:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
10:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
11:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
12:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
13:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
14:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
15:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
16:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
17:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
18:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
19:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
20:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
21:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
22:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
23:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
24:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890
25:1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890-1234567890"""
}
