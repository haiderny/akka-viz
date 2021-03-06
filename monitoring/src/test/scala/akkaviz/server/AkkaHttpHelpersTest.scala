package akkaviz.server

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.Marshalling.WithFixedContentType
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration._

class AkkaHttpHelpersTest extends FunSuite with Matchers with ScalaFutures with ScalatestRouteTest {

  import AkkaHttpHelpers._

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private[this] implicit val system: ActorSystem = ActorSystem()
  private[this] implicit val materializer = ActorMaterializer()(system)
  private[this] implicit val marshaller = Marshaller.strict[Int, String] {
    received =>
      WithFixedContentType(MediaTypes.`application/json`, () => String.valueOf(received))
  }

  test("Should work for empty Source") {
    whenReady(Source.empty[Int].via(asJsonArray).map(_.data.utf8String).runReduce(_ + _)) {
      _ shouldBe "[]"
    }
  }

  test("Should work for single element in Source") {
    whenReady(Source.single(1).via(asJsonArray).map(_.data.utf8String).runReduce(_ + _)) {
      _ shouldBe "[1]"
    }
  }

  test("Should work for multiple elements element in Source") {
    whenReady(Source(List(1, 2, 3)).via(asJsonArray).map(_.data.utf8String).runReduce(_ + _)) {
      _ shouldBe "[1,2,3]"
    }
  }

  test("asJsonArray is incremental") {
    val (pub, sub) = TestSource.probe[Int]
      .via(asJsonArray)
      .map(_.data().utf8String)
      .toMat(TestSink.probe[String])(Keep.both)
      .run()

    pub.sendNext(1)
    sub.request(10)
    sub.expectNext("[")
    pub.sendNext(2)
    sub.expectNext("1")
    pub.sendNext(3)
    sub.expectNext(",2")
    pub.sendComplete()
    sub.expectNext(",3")
    sub.expectNext("]")
    sub.expectComplete()
  }

  test("completeAsJson works properly") {
    val source = Source(List(1, 2, 3))

    Get() ~> completeAsJson(source) ~> check {
      chunks should have size (5)
      responseAs[String] shouldEqual "[1,2,3]"
    }
  }

}
