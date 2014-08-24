package online.auctionbids

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import online.auctionbids.engine.{AuctionConfig, Auctioneer, Bidder, Item}

/**
 * @author Diptamay Sanyal
 * @version 1.0
 */
object AuctionRunner extends App {

  val random      = new scala.util.Random
  val system      = ActorSystem("auctions")
  val noOfItems   = 1
  val noOfBidders = 2

  // Create items
  val items = for (i <- 1 to noOfItems) yield Item(UUID.randomUUID().toString, i * random.nextInt(100))

  // Create 'Auctioneer' actor per item
  val auctioneers = for {
    item <- items
  } yield system.actorOf(Props(classOf[AuctioneerActor], system, Auctioneer("auctioneer"), item))

  // Create 2 bidders for each item
  val bidderList = for (i <- 1 to noOfBidders) yield Bidder("bidder" + (i + 1))
  val bidders    = for {
    item <- items
    bidder <- bidderList
  } yield system.actorOf(Props(classOf[BidderActor], system, bidder, item, item.reservedPrice * 2))

  auctioneers.foreach(_ ! AuctionItem)
  bidders.foreach(_ ! BidItem)

  Thread.sleep(AuctionConfig.AUCTION_DURATION * 1000)
  auctioneers.foreach(_ ! CallAuction)

  Thread.sleep(AuctionConfig.AUCTION_DURATION * 1000)
  system.shutdown()
}

