package online.auctionbids

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import online.auctionbids.engine.{Auctioneer, Bidder, Item}

/**
 * @author Diptamay Sanyal
 * @version 1.0
 */
object AuctionRunner extends App {

  val random = new scala.util.Random
  val system = ActorSystem("auction-house")

  // Create 3 items
  val items = for (i <- 1 to 3) yield Item(UUID.randomUUID().toString, i * random.nextInt(100))

  // Create 'Auctioneer' actor per item
  val auctioneers = for {
    item <- items
  } yield system.actorOf(Props(classOf[AuctioneerActor], system, Auctioneer("auctioneer"), item))

  // Create 5 bidders for each item
  val bidderList = for (i <- 1 to 5) yield Bidder("bidder" + (i + 1))
  val bidders = for {
    item <- items
    bidder <- bidderList
  } yield system.actorOf(Props(classOf[BidderActor], system, bidder, item, item.reservedPrice * random.nextInt(2)))

  auctioneers.foreach(_ ! AuctionItem)
  bidders.foreach(_ ! BidItem)
}

