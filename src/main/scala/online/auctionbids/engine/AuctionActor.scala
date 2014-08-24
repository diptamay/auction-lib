package online.auctionbids.engine

import akka.actor.Actor
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * @author Diptamay Sanyal
 * @version 1.0
 */
class AuctionActor extends Actor {

  type KVStore[K, V] = mutable.Map[K, V]
  val auctions: KVStore[String, Auction] = mutable.Map()

  def receive: Receive = {
    case Add(item, auctioneer) => sender() ! addAuction(item, auctioneer)
    case Start(item, auctioneer) => sender() ! startAuction(item, auctioneer)
    case Call(item, auctioneer) => sender() ! callAuction(item, auctioneer)
    case Inquire(item, bidderOpt) => sender() ! inquire(item, bidderOpt)
    case Offer(auction, bid, bidder) => sender() ! offerBid(auction, bid, bidder)
  }

  def inquire(item: Item, bidderOpt: Option[Bidder] = None): AuctionReply = {
    val auctionOpt = auctions.get(item.name)
    val reply = auctionOpt match {
      case Some(auction) if bidderOpt != None && auction.highestBidder != None && auction.highestBidder.get == bidderOpt.get =>
        auction.status match {
          case AuctionStatus.Succeeded => Winner(auction.copy())
          case AuctionStatus.Running => BestOffer(auction.copy())
          case _ => Status(auction.copy(highestBid = None, highestBidder = None))
        }
      case Some(auction) =>
        auction.status match {
          case AuctionStatus.Succeeded => Status(auction.copy())
          case _ => Status(auction.copy(highestBid = None, highestBidder = None))
        }
      case _ => NotFound
    }
    reply
  }

  def addAuction(item: Item, auctioneer: Auctioneer): AuctionReply = {
    val auctionOpt = auctions.get(item.name)
    val reply = auctionOpt match {
      case Some(auction) => inquire(auction.item)
      case None =>
        auctions += (item.name -> Auction(item.copy(), auctioneer))
        Status(auctions.get(item.name).get.copy())
    }
    reply
  }

  def startAuction(item: Item, auctioneer: Auctioneer): AuctionReply = {
    var auctionOpt = auctions.get(item.name)
    val reply = auctionOpt match {
      case Some(auction) if auction.auctioneer == auctioneer && auction.status == AuctionStatus.NotStarted =>
        auctions += (auction.item.name -> auction.copy(status = AuctionStatus.Running, startedAt = Option(DateTime.now())))
        auctionOpt = auctions.get(item.name)
        Status(auctions.get(item.name).get.copy())
      case _ => inquire(item)
    }
    reply
  }

  def callAuction(item: Item, auctioneer: Auctioneer): AuctionReply = {
    val auctionOpt = auctions.get(item.name)
    val reply = auctionOpt match {
      case Some(auction) if auction.auctioneer == auctioneer && auction.status == AuctionStatus.Running =>
        val finalStatus = if (auction.highestBid.get > item.reservedPrice) AuctionStatus.Succeeded else AuctionStatus.Failed
        auctions += (auction.item.name -> auction.copy(status = finalStatus, closedAt = Option(DateTime.now())))
        finalStatus match {
          case AuctionStatus.Succeeded => Status(auctions.get(item.name).get.copy())
          case _ => Status(auctions.get(item.name).get.copy(highestBid = None, highestBidder = None))
        }
      case _ => inquire(item)
    }
    reply
  }

  def offerBid(item: Item, bid: Double, bidder: Bidder): AuctionReply = {
    val auctionOpt = auctions.get(item.name)
    val reply = auctionOpt match {
      case Some(auction) =>
        auction.status match {
          case AuctionStatus.Running if bid > auction.highestBid.get =>
            auctions += (auction.item.name -> auction.copy(highestBid = Option(bid), highestBidder = Option(bidder)))
            BestOffer(auction.copy())
          case AuctionStatus.Running if bid <= auction.highestBid.get =>
            LowOffer(auction.copy(highestBidder = None))
          case _ => inquire(item, Option(bidder))
        }
      case _ => inquire(item, Option(bidder))
    }
    reply
  }
}