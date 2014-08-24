package online.auctionbids.engine

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging}
import online.auctionbids.engine.KVStore.auctions
import org.joda.time.DateTime

/**
 * @author Diptamay Sanyal
 * @version 1.0
 */
class AuctionActor extends Actor with ActorLogging {

  def invokeCommand[A](callCommand: => A) = {
    val reply = callCommand
    log.debug(s"Sending $reply to $sender()")
    sender() ! reply
  }

  def receive: Receive = {
    case Add(item, auctioneer) => invokeCommand {
      addAuction(item, auctioneer)
    }
    case Start(item, auctioneer) => invokeCommand {
      startAuction(item, auctioneer)
    }
    case Call(item, auctioneer) => invokeCommand {
      callAuction(item, auctioneer)
    }
    case Inquire(item, bidderOpt) => invokeCommand {
      inquire(item, bidderOpt)
    }
    case Offer(auction, bid, bidder) => invokeCommand {
      offerBid(auction, bid, bidder)
    }
    case _ => log.warning("Unknown message")
  }

  def inquire(item: Item, bidderOpt: Option[Bidder] = None): AuctionReply = {
    log.debug(s"Inquiring $item")
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
    log.debug(s"$auctioneer adding $item for auctioning")
    val auctionOpt = auctions.get(item.name)
    val reply = auctionOpt match {
      case Some(auction) => inquire(auction.item)
      case None =>
        val newItem = item.copy()
        auctions += (item.name -> Auction(newItem, auctioneer))
        log.info(s"*** ADDED $newItem TO AUCTIONS ***")
        Status(auctions.get(item.name).get.copy())
    }
    reply
  }

  def startAuction(item: Item, auctioneer: Auctioneer): AuctionReply = {
    log.debug(s"$auctioneer starting auction for $item")
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
    log.debug(s"$auctioneer ending auction for $item")
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
    log.debug(s"$bidder bidding for $item with bid $bid")
    val auctionOpt = auctions.get(item.name)
    val reply = auctionOpt match {
      case Some(auction) =>
        auction.status match {
          case AuctionStatus.Running if auction.highestBid != None && bid > auction.highestBid.get =>
            auctions += (auction.item.name -> auction.copy(highestBid = Option(bid), highestBidder = Option(bidder)))
            BestOffer(auction.copy())
          case AuctionStatus.Running if bid > AuctionConfig.MIN_BID_VALUE =>
            auctions += (auction.item.name -> auction.copy(highestBid = Option(bid), highestBidder = Option(bidder)))
            BestOffer(auction.copy())
          case _ => inquire(item, Option(bidder))
        }
      case _ => inquire(item, Option(bidder))
    }
    reply
  }
}