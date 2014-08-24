package online.auctionbids.engine

import online.auctionbids.engine.AuctionStatus.AuctionStatus
import org.joda.time.DateTime

/**
 * @author Diptamay Sanyal
 * @version 1.0
 */
object AuctionStatus extends Enumeration {
  type AuctionStatus = Value
  val NotStarted, Running, Succeeded, Failed = Value
}

abstract class Participant(name: String)

case class Auctioneer(name: String) extends Participant(name)

case class Bidder(name: String) extends Participant(name)

case class Item(name: String, reservedPrice: Double = 0.0)

case class Auction(item: Item,
                   auctioneer: Auctioneer,
                   status: AuctionStatus = AuctionStatus.NotStarted,
                   startedAt: Option[DateTime] = None,
                   closedAt: Option[DateTime] = None,
                   highestBid: Option[Double] = None,
                   highestBidder: Option[Bidder] = None)

object AuctionConfig {
  final val AUCTION_DURATION                 = 5
  final val AUCTION_START_DELAY              = 1000
  final val AUCTION_CHECK_DELAY              = 500
  final val BIDDER_TIME_TO_THINK_ABOUT_OFFER = 1000
  final val MIN_BID_VALUE                    = 1
}


sealed trait AuctionMessage
case class Add(item: Item, auctioneer : Auctioneer) extends AuctionMessage // Add Item for auction
case class Start(item: Item, auctioneer : Auctioneer) extends AuctionMessage // Auctioneer starts the auction
case class Call(item: Item, auctioneer : Auctioneer) extends AuctionMessage // Auctioneer calls the auction
case class Inquire(item: Item, bidder: Option[Bidder] = None) extends AuctionMessage // a bidder/auctioneer requests the status of the auction
case class Offer(item: Item, bid: Double, bidder: Bidder) extends AuctionMessage // Bidder makes a bid

sealed trait AuctionReply
case object NotFound extends AuctionReply // auction/item not found
case class Status(auction: Auction) extends AuctionReply // auction replies the status
case class Winner(auction: Auction) extends AuctionReply // Bidder is the auction winner
case class BestOffer(auction: Auction) extends AuctionReply // Bidder has the best offer

