import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import scala.collection.mutable.HashMap
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout

case class Add(key: String)
case class Scan(max: Int)

// just worker actors
class UserActor extends Actor {
  override def receive = {
    case _ => // NOP
  }
}

// We think about parents for two pattern, so create a trait to share common logics
trait UserManager extends Actor {
  protected def create(key: String) = context.actorOf(Props[UserActor], key)
  protected def add(key: String): Unit
  protected def send(key: String, value: Any): Unit

  override def receive = {
    case Add(key)  => add(key)
    case Scan(max) =>
      for( i <- 1 to max) { send(i.toString, "hello") }
      sender() ! "done"
  }
}

// Parent(A): keeps user refs directly in his HashMap
class DirectActor extends UserManager {
  val users = HashMap[String, ActorRef]()
  protected def add(key: String) { users.update(key, create(key)) }
  protected def send(key: String, value: Any) = users.get(key) match {
    case Some(ref) => ref ! value
    case None      => throw new RuntimeException(s"$key not found")
  }
}

// Parent(B): finds users on the fly
class LookupActor extends UserManager {
  protected def add(key: String) { create(key) }
  protected def send(key: String, value: Any) = context.actorSelection(key) ! value
}

/*
 * @example {
 *   sbt -mem 4096 "run direct 100000"
 *   sbt -mem 4096 "run lookup 100000"
 * }
 */
object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("test")
    args.toList match {
      case "direct" :: i :: Nil => run(system.actorOf(Props[DirectActor]), i.toInt)
      case "lookup" :: i :: Nil => run(system.actorOf(Props[LookupActor]), i.toInt)
      case _ => println("usage: run (direct|lookup) nrMax")
    }
    system.shutdown
    system.awaitTermination(60.seconds)
  }

  def run(manager: ActorRef, max: Int) {
    for(i <- 1 to max) manager ! Add(i.toString)
      
    val msec = time {
      execute(manager, max)
    }
    report(max, msec)
  }

  def report(max: Int, msec: Long) {
    println(s"${max/1000}K $msec msec")
  }

  def execute(ref: ActorRef, max: Int) {
    implicit val timeout = Timeout(10000)
    val asked = Await.result((ref ? Scan(max)).mapTo[String], timeout.duration)
    asked match {
      case "done" => // OK
      case msg => throw new RuntimeException(s"execution failed: $msg")
    }
  }

  private def time[A](a: => A): Long = {
    val now = System.nanoTime
    val result = a
    val msec = (System.nanoTime - now) / 1000 / 1000
    return msec
  }
}

