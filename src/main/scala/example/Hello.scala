package example

import akka.http.scaladsl.server.Route
import example.Endpoints._
import io.circe.generic.auto._
import tapir._
import tapir.docs.openapi._
import tapir.json.circe._
import tapir.model.StatusCode
import tapir.openapi.OpenAPI
import tapir.openapi.circe.yaml._
import tapir.server.akkahttp._

import scala.concurrent.Future

case class SimpleRequest(id: Long, param: String)

case class SimpleResponse(id: Long, value: String)

case class ErrorInfo(error: String)

sealed trait ComplexRequest

case class Position(prdId: Long, qty: Double)

case class Foo(id: Int, arg: String)

final case class ComplexRequest1(tag: String, date: String, foo: Foo)
    extends ComplexRequest
//final case class ComplexRequest2(tag: String, date: String)
//    extends ComplexRequest

object Endpoints {
  val baseEndpoint: Endpoint[Unit, (StatusCode, ErrorInfo), Unit, Nothing] =
    endpoint
      .in("api" / "v1.0")
      .errorOut(statusCode.and(jsonBody[ErrorInfo]))

  val simple: Endpoint[SimpleRequest,
                       (StatusCode, ErrorInfo),
                       SimpleResponse,
                       Nothing] = baseEndpoint.post
    .in("simple")
    .in(jsonBody[SimpleRequest].example(SimpleRequest(42, "whatever")))
    .out(jsonBody[SimpleResponse].example(SimpleResponse(1337, "duh")))

  val complex = baseEndpoint.post
    .in("complex")
    .in(jsonBody[ComplexRequest])
    .out(jsonBody[SimpleResponse])
}

object AkkaRoutes {
  val simpleRoute: Route = Endpoints.simple.toRoute { request =>
    Future.successful(
      Right(SimpleResponse(id = request.id, value = request.param))
    )
  }

  val complexRoute = Endpoints.complex.toRoute { request =>
    Future.successful(Right(SimpleResponse(1, "11")))
  }
}

object Documentation {
  val openApi: OpenAPI =
    List(simple, complex).toOpenAPI("The tapir library", "0.29.192-beta-RC1")
  val yml: String = openApi.toYaml
}

class SwaggerUI(yml: String) {
  import java.util.Properties

  import akka.http.scaladsl.model.StatusCodes
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server.Route

  val DocsYml = "docs.yml"

  private val redirectToIndex: Route =
    redirect(
      s"/docs/index.html?url=/docs/$DocsYml",
      StatusCodes.PermanentRedirect
    ) //

  private val swaggerUiVersion = {
    val p = new Properties()
    p.load(
      getClass.getResourceAsStream(
        "/META-INF/maven/org.webjars/swagger-ui/pom.properties"
      )
    )
    p.getProperty("version")
  }

  val routes: Route =
    path("docs") {
      redirectToIndex
    } ~
      pathPrefix("docs") {
        path("") { // this is for trailing slash
          redirectToIndex
        } ~
          path(DocsYml) {
            complete(yml)
          } ~
          getFromResourceDirectory(
            s"META-INF/resources/webjars/swagger-ui/$swaggerUiVersion/"
          )
      }
}

object Hello extends App {

  def startServer(): Unit = {

    import AkkaRoutes._
    import akka.actor.ActorSystem
    import akka.http.scaladsl.Http
    import akka.http.scaladsl.server.Directives._
    import akka.http.scaladsl.server.Route
    import akka.stream.ActorMaterializer

    import scala.concurrent.Await
    import scala.concurrent.duration._

    val routes: Route = simpleRoute ~ complexRoute ~ new SwaggerUI(Documentation.yml).routes
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    Await.result(Http().bindAndHandle(routes, "localhost", 8080), 1.minute)
    println("Server started, visit http://localhost:8080/docs for the API docs")
  }

  println("Started")
  startServer()
}
