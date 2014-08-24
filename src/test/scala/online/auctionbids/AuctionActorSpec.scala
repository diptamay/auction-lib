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
  val item3           = Item("test-item-3", 30.0)
  val auctioneer1     = Auctioneer("test-auctioneer-1")
  val bidder1         = Bidder("test-bidder-1")
  val bidder2         = Bidder("test-bidder-2")
  val auctionActorRef = TestActorRef(new AuctionActor)

  override def afterAll() {
    shutdown()
  }

  "Auctioneer" should {
    "add item to auctions" in {
      auctionActorRef ? Add(item1, auctioneer1) map {
        case Status(auction) =>
          assert(auction.status == NotStarted)
          assert(auction.item == item1)
          assert(auction.auctioneer == auctioneer1)
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "start auction" in {
      auctionActorRef ? Start(item1, auctioneer1) map {
        case Status(auction) =>
          assert(auction.status == Running)
          assert(auction.startedAt != None)
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "inquire valid auction and get back status" in {
      auctionActorRef ? Inquire(item1) map {
        case Status(auction) =>
          assert(auction.status == Running)
          assert(auction.highestBid == None)
          assert(auction.highestBidder == None)
          assert(auction.closedAt == None)
        case _ => fail()
      }
    }
  }

  "Bidder" should {
    "inquire valid auction and get back status" in {
      auctionActorRef ? Inquire(item1, Option(bidder1)) map {
        case Status(auction) =>
          assert(auction.status == Running)
          assert(auction.highestBid == None)
          assert(auction.highestBidder == None)
          assert(auction.closedAt == None)
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "call auction" in {
      auctionActorRef ? Call(item1, auctioneer1) map {
        case Status(auction) =>
          assert(auction.status == Failed)
          assert(auction.highestBid == None)
          assert(auction.highestBidder == None)
          assert(auction.closedAt != None)
        case _ => fail()
      }
    }
  }

  "Bidder" should {
    "inquire concluded auction and get back status" in {
      auctionActorRef ? Inquire(item1, Option(bidder1)) map {
        case Status(auction) =>
          assert(auction.status == Failed)
          assert(auction.highestBid == None)
          assert(auction.highestBidder == None)
          assert(auction.closedAt != None)
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "inquire invalid auction and get back not found" in {
      auctionActorRef ? Inquire(item2) map {
        case NotFound =>
        case _ => fail()
      }
    }
  }

  "Bidder" should {
    "inquire invalid auction and get back not found" in {
      auctionActorRef ? Inquire(item2, Option(bidder1)) map {
        case NotFound =>
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "not be able to add already auction item" in {
      auctionActorRef ? Add(item1, auctioneer1) map {
        case Status(auction) =>
          assert(auction.status != NotStarted)
          assert(auction.item == item1)
          assert(auction.auctioneer == auctioneer1)
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "be able to add another item for auctions" in {
      auctionActorRef ? Add(item2, auctioneer1) map {
        case Status(auction) =>
          assert(auction.status == NotStarted)
          assert(auction.item == item2)
          assert(auction.auctioneer == auctioneer1)
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "be able to start another auction" in {
      auctionActorRef ? Start(item2, auctioneer1) map {
        case Status(auction) =>
          assert(auction.status == Running)
          assert(auction.startedAt != None)
        case _ => fail()
      }
    }
  }

  "Bidder 1" should {
    "be able to bid for an active auction" in {
      auctionActorRef ? Offer(item2, item2.reservedPrice - 1, bidder1) map {
        case BestOffer(auction) =>
          assert(auction.status == Running)
          assert(auction.startedAt != None)
          assert(auction.highestBidder.get == bidder1)
        case _ => fail()
      }
    }
  }

  "Bidder 2" should {
    "also be able to bid for an active auction" in {
      auctionActorRef ? Offer(item2, item2.reservedPrice + 1, bidder2) map {
        case BestOffer(auction) =>
          assert(auction.highestBidder.get == bidder2)
        case _ => fail()
      }
    }
  }

  "Bidder 1" should {
    "be able to inquire and find its not best offer anymore" in {
      auctionActorRef ? Inquire(item2, Option(bidder1)) map {
        case Status(auction) =>
          assert(auction.status == Running)
          assert(auction.highestBidder.get != bidder1)
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "not be able to start invalid auction" in {
      auctionActorRef ? Start(item3, auctioneer1) map {
        case NotFound =>
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "be able to add more item for auctions" in {
      auctionActorRef ? Add(item3, auctioneer1) map {
        case Status(auction) =>
          //println(s"Got auction status $auction")
          assert(auction.status == NotStarted)
          assert(auction.item == item3)
          assert(auction.auctioneer == auctioneer1)
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "be able to start another active auction" in {
      auctionActorRef ? Start(item3, auctioneer1) map {
        case Status(auction) =>
          //println(s"Got auction status $auction")
          assert(auction.status == Running)
          assert(auction.item == item3)
          assert(auction.startedAt != None)
        case _ => fail()
      }
    }
  }

  "Bidder 1" should {
    "also be able to bid for another active auction" in {
      auctionActorRef ? Offer(item3, item3.reservedPrice + 1, bidder1) map {
        case BestOffer(auction) =>
          assert(auction.status == Running)
          assert(auction.startedAt != None)
          assert(auction.highestBidder.get == bidder1)
        case _ => fail()
      }
    }
  }

  "Bidder 2" should {
    "also be able to bid for another active auction" in {
      auctionActorRef ? Offer(item3, item3.reservedPrice - 1, bidder2) map {
        case Status(auction) =>
          assert(auction.status == Running)
          assert(auction.highestBidder.get == bidder1)
          assert(auction.highestBidder.get != bidder2)
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "call active auction" in {
      auctionActorRef ? Call(item2, auctioneer1) map {
        case Status(auction) =>
          assert(auction.status == Succeeded)
          assert(auction.highestBid != None)
          assert(auction.highestBidder.get == bidder2)
          assert(auction.closedAt != None)
        case _ => fail()
      }
    }
  }

  "Auctioneer" should {
    "call another active auction" in {
      auctionActorRef ? Call(item3, auctioneer1) map {
        case Status(auction) =>
          assert(auction.status == Succeeded)
          assert(auction.highestBid != None)
          assert(auction.highestBidder.get == bidder1)
          assert(auction.closedAt != None)
        case _ => fail()
      }
    }
  }

  "Bidder 1" should {
    "be able find out its the winner for an auction" in {
      auctionActorRef ? Inquire(item3, Option(bidder1)) map {
        case Winner(auction) =>
          assert(auction.status == Succeeded)
          assert(auction.highestBidder.get == bidder1)
        case _ => fail()
      }
    }
  }

  "Bidder 2" should {
    "be able find out its the winner for an auction" in {
      auctionActorRef ? Inquire(item2, Option(bidder2)) map {
        case Winner(auction) =>
          assert(auction.status == Succeeded)
          assert(auction.highestBidder.get == bidder2)
        case _ => fail()
      }
    }
  }
}

