package online.auctionbids

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import online.auctionbids.engine.AuctionStatus._
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
    case Status(auction) =>
      auction.status match {
        case NotStarted =>
          Thread.sleep(1 + random.nextInt(AuctionConfig.BIDDER_TIME_TO_THINK_ABOUT_OFFER))
          val item = auction.item
          log.info(s"$bidder inquiring for $item")
          auctionActor ! Inquire(item, Option(bidder))
        case Running =>
          val bidOffer = getBidOffer(auction)
          log.info(s"$bidder bidding for $auction with offer $bidOffer")
          auctionActor ! Offer(auction.item, bidOffer, bidder)
        case _ =>
          log.info(s"State of auction is $auction")
          context.stop(self)
      }
    case NotFound => auctionActor ! Inquire(item, Option(bidder))
    case _ => log.warning("Unknown message")
  }

  def getBidOffer(auction: Auction): Double = {
    log.debug(s"maxBid set at $maxBid")
    auction.highestBid match {
      case Some(bid) if maxBid - bid > 0 => bid + random.nextInt((maxBid - bid).toInt)
      case None => random.nextInt(maxBid.toInt)
    }
  }

}