/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.util

import scala.collection.immutable.IntMap

/**
 * Trie implementation for looking up strings that allows default values
 * using a wildcard (*). All defaults on a path are included using `getAll`.
 */
class Trie[T](val map: IntMap[Trie[T]], val value: Option[T], val isWild: Boolean) {
  import Trie.{ Wildcard, Wild }

  def apply(key: String): T = get(key).get

  def get(key: String): Option[T] = Trie.find(key, this, Seq.empty)

  def getAll(key: String): Set[T] = Trie.findAll(key, this, Seq.empty, Set.empty)

  def add(key: String, newValue: T, wild: Boolean): Trie[T] = Trie.add(this, key, newValue, wild)

  def +(kv: (String, T)) = add(kv._1, kv._2, false)
}

object Trie {
  case class Fallback[T](key: String, trie: Trie[T])

  final val Wild = -1 // magic wild number

  val Wildcard = '*'.toInt
  val Escape = '\\'.toInt

  def empty[T] = new Trie[T](IntMap.empty[Trie[T]], None, false)

  def apply[T](kvs: (String, T)*): Trie[T] = {
    var trie: Trie[T] = empty
    for (kv ← kvs) trie += kv
    trie
  }

  def add[T](trie: Trie[T], key: String, value: T, isWild: Boolean): Trie[T] = {
    if (key.isEmpty) {
      new Trie(trie.map, Some(value), isWild)
    } else {
      val (leading, rest) = takeLeading(key)
      val next = trie.map get (leading) getOrElse (empty)
      new Trie(trie.map + (leading -> add(next, rest, value, leading == Wild)), trie.value, trie.isWild)
    }
  }

  def takeLeading(key: String): (Int, String) = {
    val first = key(0).toInt
    val rest = key substring 1
    if (first == Escape) {
      if (!rest.isEmpty && rest(0).toInt == Wildcard) (Wildcard, rest substring 1)
      else (Escape, rest)
    } else if (first == Wildcard) {
      (Wild, rest)
    } else {
      (first, rest)
    }
  }

  @annotation.tailrec
  def find[T](key: String, trie: Trie[T], fallbacks: Seq[Fallback[T]]): Option[T] = {
    if (key.isEmpty && trie.value.isDefined) trie.value
    else if (key.isEmpty) fallbackTo(addFallback(key, trie, fallbacks))
    else {
      val leading = key(0).toInt
      if (trie.map.isDefinedAt(leading)) {
        find(key substring 1, trie.map(leading), addFallback(key, trie, fallbacks))
      } else if (trie.isWild) {
        find(key substring 1, trie, fallbacks)
      } else if (trie.map.isDefinedAt(Wild)) {
        find(key, trie.map(Wild), fallbacks)
      } else {
        fallbackTo(fallbacks)
      }
    }
  }

  def addFallback[T](key: String, trie: Trie[T], fallbacks: Seq[Fallback[T]]): Seq[Fallback[T]] = {
    if (!trie.map.isDefinedAt(Wild)) fallbacks
    else if (fallbacks.isEmpty) Seq(Fallback(key, trie.map(Wild)))
    else Fallback(key, trie.map(Wild)) +: fallbacks
  }

  def fallbackTo[T](fallbacks: Seq[Fallback[T]]): Option[T] = {
    if (fallbacks.isEmpty) None
    else find(fallbacks.head.key, fallbacks.head.trie, fallbacks.tail)
  }

  @annotation.tailrec
  def findAll[T](key: String, trie: Trie[T], fallbacks: Seq[Fallback[T]], values: Set[T]): Set[T] = {
    if (key.isEmpty && trie.value.isDefined) fallbackToAll(fallbacks, trie.value.toSet ++ values)
    else if (key.isEmpty) fallbackToAll(addFallback(key, trie, fallbacks), values)
    else {
      val leading = key(0).toInt
      if (trie.map.isDefinedAt(leading)) {
        findAll(key substring 1, trie.map(leading), addFallback(key, trie, fallbacks), values)
      } else if (trie.isWild) {
        findAll(key substring 1, trie, fallbacks, trie.value.toSet ++ values)
      } else if (trie.map.isDefinedAt(Wild)) {
        findAll(key, trie.map(Wild), fallbacks, values)
      } else {
        fallbackToAll(fallbacks, values)
      }
    }
  }

  def fallbackToAll[T](fallbacks: Seq[Fallback[T]], values: Set[T]): Set[T] = {
    if (fallbacks.isEmpty) values
    else findAll(fallbacks.head.key, fallbacks.head.trie, fallbacks.tail, values)
  }
}
