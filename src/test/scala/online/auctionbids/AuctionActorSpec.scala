package online.auctionbids

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import online.auctionbids.engine.AuctionStatus._
import online.auctionbids.engine._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global

class AuctionActorSpec extends TestKit(ActorSystem("testSystem")) with WordSpecLike with Matchers with DefaultTimeout
                               with ImplicitSender with BeforeAndAfterAll {

  val item1           = Item("test-item-1", 10.0)
  val item2           = Item("test-item-2", 20.0)
  val auctioneer1     = Auctioneer("test-auctioneer-1")
  val bidder1         = Bidder("test-bidder-1")
  val bidder2         = Bidder("test-bidder-2")
  val auctionActorRef = TestActorRef(new AuctionActor)

  override def afterAll() {
    shutdown()
  }

  "Auctioneer should" should {
    "add item to auctions" in {
      auctionActorRef ? Add(item1, auctioneer1) map {
        case Status(auction) =>
          println(s"Got auction status $auction")
          assert(auction.status == NotStarted)
          assert(auction.item == item1)
          assert(auction.auctioneer == auctioneer1)
        case _ => assert(false)
      }
    }
  }

  "Auctioneer should" should {
    "start auction" in {
      auctionActorRef ? Start(item1, auctioneer1) map {
        case Status(auction) =>
          println(s"Got auction status $auction")
          assert(auction.status == Running)
          assert(auction.startedAt != None)
        case _ => assert(false)
      }
    }
  }

  "Auctioneer should" should {
    "inquire valid auction and get back status" in {
      auctionActorRef ? Inquire(item1) map {
        case Status(auction) =>
          println(s"Got auction status $auction")
          assert(auction.status == Running)
          assert(auction.highestBid == None)
          assert(auction.highestBidder == None)
          assert(auction.closedAt == None)
        case _ => assert(false)
      }
    }
  }

  "Auctioneer should" should {
    "call auction" in {
      auctionActorRef ? Call(item1, auctioneer1) map {
        case Status(auction) =>
          println(s"Got auction status $auction")
          assert(auction.status == Failed)
          assert(auction.highestBid == None)
          assert(auction.highestBidder == None)
          assert(auction.closedAt != None)
        case _ => assert(false)
      }
    }
  }

  "Auctioneer should" should {
    "inquire invalid auction and get back not found" in {
      auctionActorRef ? Inquire(item2) map {
        case NotFound => assert(true)
        case _ => assert(false)
      }
    }
  }

  "Auctioneer should" should {
    "not be able to add already auction item" in {
      auctionActorRef ? Add(item1, auctioneer1) map {
        case Status(auction) =>
          println(s"Got auction status $auction")
          assert(auction.status != NotStarted)
          assert(auction.item == item1)
          assert(auction.auctioneer == auctioneer1)
        case _ => assert(false)
      }
    }
  }

  "Auctioneer should" should {
    "not be able to start an already auction item" in {
      auctionActorRef ? Start(item1, auctioneer1) map {
        case Status(auction) =>
          println(s"Got auction status $auction")
          assert(auction.status != Running)
          assert(auction.item == item1)
          assert(auction.auctioneer == auctioneer1)
        case _ => assert(false)
      }
    }
  }
}

