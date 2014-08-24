package online.auctionbids

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import online.auctionbids.engine._

/**
 * @author Diptamay Sanyal
 * @version 1.0
 */
case object BidItem

class BidderActor(actorSystem: ActorSystem, bidder: Bidder, item: Item, maxBid: Double) extends Actor with ActorLogging {

  val random       = new scala.util.Random
  val auctionActor = actorSystem.actorOf(Props[AuctionActor])

  def receive: Receive = {
    case BidItem => auctionActor ! Inquire(item, Option(bidder))
    case Winner(auction) =>
      log.info(s"$bidder is the winner of $auction.item")
      context.stop(self)
    case BestOffer(auction) =>
      log.info(s"$bidder has the best offer for $auction.item")
      Thread.sleep(1 + random.nextInt(AuctionConfig.BIDDER_TIME_TO_THINK_ABOUT_OFFER))
      auctionActor ! Inquire(auction.item, Option(bidder))
    case LowOffer(auction) =>
      log.info(s"$bidder has to make better offer for $auction.item")
      if (auction.highestBid.get < maxBid) {
        val bidOffer = getBidOffer(auction)
        auctionActor ! Offer(auction.item, bidOffer, bidder)
      } else {
        log.info(s"$bidder can't make better offer for $auction.item")
        context.stop(self)
      }
    case Status(auction) =>
      auction.status match {
        case AuctionStatus.NotStarted =>
          Thread.sleep(1 + random.nextInt(AuctionConfig.BIDDER_TIME_TO_THINK_ABOUT_OFFER))
          auctionActor ! Inquire(auction.item, Option(bidder))
        case AuctionStatus.Running =>
          val bidOffer = getBidOffer(auction)
          auctionActor ! Offer(auction.item, bidOffer, bidder)
        case _ =>
          log.info(s"State of auction is $auction")
          context.stop(self)
      }
    case NotFound => context.stop(self)
  }

  def getBidOffer(auction: Auction): Double = auction.highestBid.get + random.nextInt((maxBid - auction.highestBid.get).toInt)
}