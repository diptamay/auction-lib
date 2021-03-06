package online.auctionbids

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import online.auctionbids.engine.AuctionStatus._
import online.auctionbids.engine._
import org.joda.time.{DateTime, Seconds}

/**
 * @author Diptamay Sanyal
 * @version 1.0
 */
case object AuctionItem

case object StartAuction

case object CallAuction

class AuctioneerActor(actorSystem: ActorSystem, auctioneer: Auctioneer, item: Item) extends Actor with ActorLogging {

  val random       = new scala.util.Random
  val auctionActor = actorSystem.actorOf(Props[AuctionActor])

  def receive: Receive = {
    case AuctionItem =>
      log.info(s"$auctioneer auctioning $item")
      auctionActor ! Add(item, auctioneer)
    case StartAuction =>
      log.info(s"$auctioneer starting auction for $item")
      auctionActor ! Start(item, auctioneer)
    case CallAuction =>
      log.info(s"$auctioneer calling auction for $item")
      auctionActor ! Call(item, auctioneer)
    case Status(auction) =>
      auction.status match {
        case NotStarted =>
          Thread.sleep(1 + random.nextInt(AuctionConfig.AUCTION_START_DELAY))
          log.info(s"$auctioneer starting auction for $item")
          auctionActor ! Start(item, auctioneer)
        case Running =>
          val seconds = Seconds.secondsBetween(DateTime.now(), auction.startedAt.get).getSeconds
          if (seconds < AuctionConfig.AUCTION_DURATION) {
            Thread.sleep(1 + random.nextInt(AuctionConfig.AUCTION_CHECK_DELAY))
            log.info(s"$auctioneer inquiring for $item")
            auctionActor ! Inquire(auction.item)
          } else {
            log.info(s"$auctioneer calling auction for $item")
            auctionActor ! Call(item, auctioneer)
          }
        case _ =>
          log.info(s"State of auction is $auction")
          context.stop(self)
      }
    case NotFound => auctionActor ! Inquire(item)
    case _ => log.warning("Unknown message")
  }
}
