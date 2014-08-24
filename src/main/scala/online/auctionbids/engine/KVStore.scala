package online.auctionbids.engine

import scala.collection.mutable

/**
 * @author Diptamay Sanyal
 * @version 1.0
 */
object KVStore {
  type KVStore[K, V] = mutable.Map[K, V]
  val auctions: KVStore[String, Auction] = mutable.Map()
}
