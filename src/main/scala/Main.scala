import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import scala.collection.mutable.HashMap
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout

case class Ack(id: Int, msg: Any)
case class Add(id: Int)
case class MAdd(max: Int)
case class Send(max: Int)
case class Done(name: String, msg: String)
case class Wait(ref: ActorRef)

trait ReceiveTracer extends Actor {
 override def aroundReceive(receive: Receive, msg: Any): Unit = {
    println(s"${self.path.name}: got $msg")
    super.aroundReceive(receive, msg)
  }
}

// just worker actors
class UserActor(id: Int) extends Actor with ReceiveTracer {
  override def receive = {
    case msg =>
      sender() ! Ack(id, msg)
  }
}

// We think about parents for two pattern, so create a trait to share common logics
trait UserManager extends Actor with ReceiveTracer {
  protected def create(id: Int) = context.actorOf(Props(classOf[UserActor], id), id.toString)
  protected def add(id: Int): Unit
  protected def send(id: Int, value: Any): Unit
  protected var ackCount = 0

  override def receive = {
    case Ack(id, _) => ackCount += 1; received(id)
    case Add(id)    => add(id)
    case MAdd(max)  => for(i <- 1 to max) add(i)
    case Send(max)  =>
      for(i <- 1 to max) { sended(i) }
      for(i <- 1 to max) { send(i, "hello") }
      self ! Wait(sender())
    case m @ Wait(ref) => sending.isEmpty match {
      case true => ref ! Done(self.path.name, sending.toString)
      case _ => Thread.sleep(100); self ! m
    }
  }

  private val alerted = HashMap[String, Boolean]()
  protected def alertOnce(key: String, msg: String) {
    alerted.getOrElse(key, {
      Console.err.println(s"[$key] $msg")
      alerted.update(key, true)
    })
  }

  protected val sending = HashMap[Int, Boolean]()
  protected def sended(id: Int) = sending.update(id, true)
  protected def received(id: Int) = sending.remove(id)
}

// Parent(A): keeps user refs directly in his HashMap
class DirectActor extends UserManager {
  val users = HashMap[Int, ActorRef]()
  protected def add(id: Int) { users.update(id, create(id)) }
  protected def send(id: Int, value: Any) = users.get(id) match {
    case Some(ref) => ref ! value
    case None      => alertOnce("user not found", id.toString)
  }

}

// Parent(B): finds users on the fly
class LookupActor extends UserManager {
  protected def add(id: Int) { create(id) }
  protected def send(id: Int, value: Any) = context.actorSelection(id.toString) ! value
}

/*
 * @example {
 *   sbt -mem 4096 "run direct 100000"
 *   sbt -mem 4096 "run lookup 100000"
 * or simply
 *   sbt -mem 4096 "run 100000"
 * }
 */
object Main {
  def main(args: Array[String]): Unit = {
    args.toList match {
      case "direct" :: i :: Nil => run(direct, i.toInt)
      case "lookup" :: i :: Nil => run(lookup, i.toInt)
      case             i :: Nil => run(direct, i.toInt); run(lookup, i.toInt)
      case _ => println("usage: run (direct|lookup) nrMax")
    }
    system.shutdown
    system.awaitTermination(60.seconds)
  }

  lazy val system = ActorSystem("test")
  def direct = system.actorOf(Props[DirectActor], "direct")
  def lookup = system.actorOf(Props[LookupActor], "lookup")

  def run(manager: ActorRef, max: Int) {
    manager ! MAdd(max)
      
    val msec = time {
      execute(manager, max)
    }
    println(s"[${manager.path}] ${max/1000}K $msec msec")
  }

  def execute(ref: ActorRef, max: Int) {
    implicit val timeout = Timeout(10000)
    val asked = Await.result((ref ? Send(max)), timeout.duration)
    asked match {
      case Done(name, msg) => println(s"$name finished with $msg")
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

