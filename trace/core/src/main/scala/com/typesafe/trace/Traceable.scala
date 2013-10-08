/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.util.{ CyclicCounter, Trie }
import scala.util.control.Exception._

object Traceable {
  class Sampling(val rate: Int) {
    val counter = CyclicCounter(rate)
  }

  val DefaultAkkaTraceableKey = "atmos.trace.akka.defaults.traceable"
  val DefaultAkkaTraceableFallbackKey = "atmos.trace.defaults.traceable"
  val DefaultPlayTraceableKey = "atmos.trace.play.defaults.traceable"
  val DefaultAkkaSamplingKey = "atmos.trace.akka.defaults.sampling"
  val DefaultAkkaSamplingFallbackKey = "atmos.trace.defaults.sampling"
  val DefaultPlaySamplingKey = "atmos.trace.play.defaults.sampling"
  val DefaultSystemTraceableKey = "atmos.trace.akka.defaults.system-traceable"
  val DefaultSystemFallbackTraceableKey = "atmos.trace.defaults.system-traceable"

  val AkkaTraceableKey = "atmos.trace.akka.traceable"
  val AkkaFallbackTraceableKey = "atmos.trace.traceable"
  val PlayTraceableKey = "atmos.trace.play.traceable"
  val AkkaSamplingKey = "atmos.trace.akka.sampling"
  val AkkaSamplingFallbackKey = "atmos.trace.sampling"
  val PlaySamplingKey = "atmos.trace.play.sampling"
  val TagsKey = "atmos.trace.tags"

  val RandomPlaceholder = "$random$"

  def actorTraceable(settings: Trace.Settings): ActorTraceable = {
    val systemDefaults = TraceConfig.getBooleanPairs(settings.config, DefaultSystemTraceableKey, Some(DefaultSystemFallbackTraceableKey))
    val fromConfig = TraceConfig.getBooleanPairs(settings.config, AkkaTraceableKey, Some(AkkaFallbackTraceableKey))
    new ActorTraceable(TraceConfig.getBoolean(settings.config, DefaultAkkaTraceableKey, Some(DefaultAkkaTraceableFallbackKey)).get,
      TraceConfig.getInt(settings.config, DefaultAkkaSamplingKey, Some(DefaultAkkaSamplingFallbackKey)).get,
      systemDefaults ++ fromConfig,
      TraceConfig.getIntPairs(settings.config, AkkaSamplingKey, Some(AkkaSamplingFallbackKey)),
      TraceConfig.getStringSetPairs(settings.config, TagsKey))
  }

  def playTraceable(settings: Trace.Settings): PlayTraceable = {
    new PlayTraceable(settings.config.getBoolean(DefaultPlayTraceableKey),
      settings.config.getInt(DefaultPlaySamplingKey),
      TraceConfig.getBooleanPairs(settings.config, PlayTraceableKey),
      TraceConfig.getIntPairs(settings.config, PlaySamplingKey))
  }
}

class Traceable(val defaultTraceable: Boolean,
                val defaultSamplingSeed: Int,
                val traceableFilters: Seq[(String, Boolean)],
                val traceableSamplings: Seq[(String, Int)]) {
  import Traceable.Sampling

  val defaultSampling: Sampling = new Sampling(defaultSamplingSeed)

  val traceableTrie: Trie[Boolean] = Trie(traceableFilters: _*)

  val samplingTrie: Trie[Sampling] = {
    val samplings = traceableSamplings map { kv ⇒ (kv._1, new Sampling(kv._2)) }
    Trie(samplings: _*)
  }

  /**
   * Is this id traceable?
   */
  def traceable(id: String): Boolean = {
    traceableTrie.get(id).getOrElse(defaultTraceable)
  }

  /**
   * Returns a sample rate > 0 if we should sample this trace.
   * NB: updates a sampling counter as a side-effect.
   */
  def sample(id: String): Int = {
    val sampling = getSampling(id)
    val rate = sampling.rate
    if (rate == 0 || rate == 1) rate
    else if (sampling.counter.next() == 0) rate
    else 0
  }

  /**
   * Get the Sampling associated with this id.
   */
  def getSampling(id: String): Sampling = {
    samplingTrie.get(id).getOrElse(defaultSampling)
  }
}

/**
 * Determining whether actors are traceable or should be sampled in the current
 * trace, and retrieving actor tags.
 */
class ActorTraceable(defaultTraceable: Boolean,
                     defaultSamplingSeed: Int,
                     traceableFilters: Seq[(String, Boolean)],
                     traceableSamplings: Seq[(String, Int)],
                     val traceableTags: Seq[(String, Set[String])]) extends Traceable(defaultTraceable, defaultSamplingSeed, traceableFilters, traceableSamplings) {
  val tagsTrie: Trie[Set[String]] = Trie(traceableTags: _*)

  /**
   * Returns the set of tags configured for this id.
   */
  def tags(id: String): Set[String] = {
    tagsTrie.getAll(id).flatten
  }
}

/**
 * Marker class for Play traceable configuration
 */
class PlayTraceable(defaultTraceable: Boolean,
                    defaultSamplingSeed: Int,
                    traceableFilters: Seq[(String, Boolean)],
                    traceableSamplings: Seq[(String, Int)]) extends Traceable(defaultTraceable, defaultSamplingSeed, traceableFilters, traceableSamplings)

/**
 * Helper methods for retrieving values from config.
 */
object TraceConfig {
  import com.typesafe.config.{ Config, ConfigException }
  import scala.collection.JavaConverters._

  def getBooleanPairs(config: Config, section: String, fallbackKey: Option[String] = None): Seq[(String, Boolean)] = {
    getPairs(config, section, fallbackKey, getBoolean(_, _, None))
  }

  def getIntPairs(config: Config, section: String, fallbackKey: Option[String] = None): Seq[(String, Int)] = {
    getPairs(config, section, fallbackKey, getInt(_, _, None))
  }

  def getStringPairs(config: Config, section: String, fallbackKey: Option[String] = None): Seq[(String, String)] = {
    getPairs(config, section, fallbackKey, getString(_, _, None))
  }

  def getStringSetPairs(config: Config, section: String, fallbackKey: Option[String] = None): Seq[(String, Set[String])] = {
    getPairs(config, section, fallbackKey, getStringSet(_, _, None))
  }

  def getPairs[T](config: Config, section: String, fallbackKey: Option[String], get: (Config, String) ⇒ Option[T]): Seq[(String, T)] = {
    val sectionConfig = allCatch[Config].opt(config.getConfig(section)) orElse fallbackKey.flatMap(fk ⇒ allCatch[Config].opt(config.getConfig(fk)))
    sectionConfig.foldLeft(Seq.empty[(String, T)]) {
      (_, sc) ⇒
        val keys = sc.entrySet.asScala.map(_.getKey).toSeq
        keys flatMap { key ⇒ get(sc, key).map((unquoteString(key), _)) }
    }
  }

  def get[T](config: Config, key: String, fallbackKey: Option[String], getter: (Config, String) ⇒ T): Option[T] =
    allCatch[T].opt(getter(config, key)) orElse fallbackKey.flatMap(k ⇒ allCatch[T].opt(getter(config, k)))

  def getBoolean(config: Config, key: String, fallbackKey: Option[String] = None): Option[Boolean] =
    get(config, key, fallbackKey, _.getBoolean(_))

  def getInt(config: Config, key: String, fallbackKey: Option[String] = None): Option[Int] =
    get(config, key, fallbackKey, _.getInt(_))

  def getString(config: Config, key: String, fallbackKey: Option[String] = None): Option[String] =
    get(config, key, fallbackKey, _.getString(_))

  def getStringSet(config: Config, key: String, fallbackKey: Option[String] = None): Option[Set[String]] =
    get(config, key, fallbackKey, (c, k) ⇒ allCatch[Set[String]].opt(c.getStringList(k).asScala.toSet).getOrElse(Set(c.getString(k))))

  def unquoteString(s: String): String = {
    val affix = "\""
    trimStart(trimEnd(s, affix), affix)
  }

  def trimStart(s: String, prefix: String): String = {
    if (s != null && s.startsWith(prefix)) s.substring(prefix.length) else s
  }

  def trimEnd(s: String, suffix: String): String = {
    if (s != null && s.endsWith(suffix)) s.substring(0, s.length - suffix.length) else s
  }
}
