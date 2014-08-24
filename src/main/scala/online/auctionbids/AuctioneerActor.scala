package online.auctionbids

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
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
    case AuctionItem => auctionActor ! Add(item, auctioneer)
    case StartAuction => auctionActor ! Start(item, auctioneer)
    case CallAuction => auctionActor ! Call(item, auctioneer)
    case Status(auction) =>
      auction.status match {
        case AuctionStatus.NotStarted =>
          Thread.sleep(1 + random.nextInt(AuctionConfig.AUCTION_START_DELAY))
          auctionActor ! Start(item, auctioneer)
        case AuctionStatus.Running =>
          val seconds = Seconds.secondsBetween(DateTime.now(), auction.startedAt.get).getSeconds
          if (seconds < AuctionConfig.AUCTION_DURATION) {
            Thread.sleep(1 + random.nextInt(AuctionConfig.AUCTION_CHECK_DELAY))
            auctionActor ! Inquire(auction.item)
          } else auctionActor ! Call(item, auctioneer)
        case _ =>
          log.info(s"State of auction is $auction")
          context.stop(self)
      }
    case NotFound => context.stop(self)
  }
}
