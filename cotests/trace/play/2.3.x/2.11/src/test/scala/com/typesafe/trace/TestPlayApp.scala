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
        DynamicPart("id", """[^/]+""", true))))
    private[this] lazy val GetControllers_showFiltered =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("get/filtered/"),
        DynamicPart("id", """[^/]+""", true))))
    private[this] lazy val GetControllers_showAsync =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("get/async/"),
        DynamicPart("duration", """[^/]+""", true))))
    private[this] lazy val GetControllers_showAsyncImmediate =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("get/async_immediate"))))
    private[this] lazy val GetControllers_showChunked =
      Route("GET", PathPattern(List(StaticPart(Routes.prefix),
        StaticPart(Routes.defaultPrefix),
        StaticPart("get/chunked/"),
        DynamicPart("id", """[^/]+""", true))))
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
        createInvoker(controllers.Application.index,
          HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "index", Nil, "GET", """ Home page""", Routes.prefix + """get""")).call(controllers.Application.index)
      }
      case Application_indexSampled(params) ⇒ call {
        createInvoker(controllers.Application.indexSampled,
          HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "indexSampled", Nil, "GET", """ Home page sampled""", Routes.prefix + """getSampled""")).call(controllers.Application.indexSampled)
      }
      case GetControllers_withSession(params) ⇒ call {
        createInvoker(controllers.GetControllers.getWithSession,
          HandlerDef(this.getClass.getClassLoader, "", "controllers.GetControllers", "getWithSession", Nil, "GET", "", Routes.prefix + """getWithSession""")).call(controllers.GetControllers.getWithSession)
      }
      case GetControllers_show(params) ⇒ call(params.fromPath[Int]("id", None)) { (id) ⇒
        createInvoker(controllers.GetControllers.show(id),
          HandlerDef(this.getClass.getClassLoader, "", "controllers.GetControllers", "show", Seq(classOf[Int]), "GET", """""", Routes.prefix + """get/sync/$id<[^/]+>""")).call(controllers.GetControllers.show(id))
      }
      case GetControllers_showFiltered(params) ⇒ call(params.fromPath[Int]("id", None)) { (id) ⇒
        createInvoker(controllers.GetControllers.showFiltered(id),
          HandlerDef(this.getClass.getClassLoader, "", "controllers.GetControllers", "showFiltered", Seq(classOf[Int]), "GET", """""", Routes.prefix + """get/filtered/$id<[^/]+>""")).call(controllers.GetControllers.showFiltered(id))
      }
      case GetControllers_showAsync(params) ⇒ call(params.fromPath[Int]("duration", None)) { (duration) ⇒
        createInvoker(controllers.GetControllers.showAsync(duration),
          HandlerDef(this.getClass.getClassLoader, "", "controllers.GetControllers", "showAsync", Seq(classOf[Int]), "GET", """""", Routes.prefix + """get/async/$duration<[^/]+>""")).call(controllers.GetControllers.showAsync(duration))
      }
      case GetControllers_showAsyncImmediate(params) ⇒ call {
        createInvoker(controllers.GetControllers.showAsyncImmediate,
          HandlerDef(this.getClass.getClassLoader, "", "controllers.GetControllers", "showAsyncImmediate", Nil, "GET", """""", Routes.prefix + """get/async_immediate""")).call(controllers.GetControllers.showAsyncImmediate)
      }
      case GetControllers_showChunked(params) ⇒ call(params.fromPath[Int]("id", None)) { (id) ⇒
        createInvoker(controllers.GetControllers.showChunked(id),
          HandlerDef(this.getClass.getClassLoader, "", "controllers.GetControllers", "showChunked", Seq(classOf[Int]), "GET", """""", Routes.prefix + """get/chunked/$id<[^/]+>""")).call(controllers.GetControllers.showChunked(id))
      }
      case GetControllers_showLarge(params) ⇒ call {
        createInvoker(controllers.GetControllers.showLarge,
          HandlerDef(this.getClass.getClassLoader, "", "controllers.GetControllers", "showLarge", Nil, "GET", """""", Routes.prefix + """get/large""")).call(controllers.GetControllers.showLarge)
      }
      case GetControllers_error(params) ⇒ call {
        createInvoker(controllers.GetControllers.error,
          HandlerDef(this.getClass.getClassLoader, "", "controllers.GetControllers", "error", Nil, "GET", """""", Routes.prefix + """error""")).call(controllers.GetControllers.error)
      }
      case PostControllers_post(params) ⇒ call {
        createInvoker(controllers.PostControllers.post,
          HandlerDef(this.getClass.getClassLoader, "", "controllers.PostControllers", "post", Nil, "POST", """""", Routes.prefix + """post""")).call(controllers.PostControllers.post)
      }
      case PostControllers_uploadFile(params) ⇒ call {
        createInvoker(controllers.PostControllers.uploadFile,
          HandlerDef(this.getClass.getClassLoader, "", "controllers.PostControllers", "uploadFile", Nil, "POST", """""", Routes.prefix + """uploadFile""")).call(controllers.PostControllers.uploadFile)
      }
      case PutControllers_put(params) ⇒ call {
        createInvoker(controllers.PutControllers.put,
          HandlerDef(this.getClass.getClassLoader, "", "controllers.PutControllers", "put", Nil, "PUT", """""", Routes.prefix + """put""")).call(controllers.PutControllers.put)
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

      // Deliberately using deprecated interface.
      // In Play 2.2 all action results are automatically 'async'
      // because results are wrapped in a Future
      def showAsync(duration: Int) = Action.async {
        scala.concurrent.Future {
          if (duration > 0) Thread.sleep(duration)
          Ok("invoked showAsync with: " + duration)
        }
      }

      // Deliberately using deprecated interface.
      // In Play 2.2 all action results are automatically 'async'
      // because results are wrapped in a Future
      def showAsyncImmediate = Action.async {
        scala.concurrent.Future(Ok("Async with immediate return"))
      }

      def showChunked(id: Int) = Action {
        Ok.chunked(
          Enumerator((1 to 10).map("chunk_" + _): _*).andThen(Enumerator.eof))
      }

      def showLarge = Action {
        SimpleResult(
          header = ResponseHeader(200),
          body = Enumerator(largeData.getBytes("utf-8")))
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

  val application: FakeApplication = {
    val r = FakeApplication(additionalConfiguration = Map("application.router" -> "com.typesafe.trace.TestApplication$Routes",
      "application.secret" -> "tbWgMhvWQ?mIxxjV^VQ6o4@rU2sfChxV`=B63HKJtS>W3OQMe9wNljXUef3^cQ`r",
      "logger.root" -> "OFF",
      "logger.play" -> "OFF",
      "logger.application" -> "OFF",
      "play" -> Map(
        "akka.loggers" -> Seq("com.typesafe.trace.test.TestLogger"),
        "akka.logger-startup-timeout" -> "10s",
        "akka.loglevel" -> "WARNING",
        "akka.stdout-loglevel" -> "WARNING",
        "akka.actor.retrieveBodyParserTimeout" -> "1 second",
        "akka.actor.default-dispatcher.fork-join-executor.parallelism-factor" -> "1.0",
        "akka.actor.default-dispatcher.fork-join-executor.parallelism-max" -> "24")))
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
