/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace.util

import org.scalatest.WordSpec
import org.scalatest.MustMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TrieSpec extends WordSpec with MustMatchers {

  // it could be better to use property testing with ScalaCheck for these tests

  def makeTrie(strings: String*) = Trie(strings.map(s ⇒ s -> s): _*)

  "A trie" must {

    "match literally" in {
      val trie = makeTrie("a", "ab", "j", "jk")

      trie("a") must be("a")
      trie("ab") must be("ab")
      trie("j") must be("j")
      trie("jk") must be("jk")

      trie.get("x") must be(None)
      trie.get("ax") must be(None)
      trie.get("axb") must be(None)
    }

    "match wildly at end" in {
      val trie = makeTrie("a", "*", "ab", "a*", "abc", "ab*", "b*")

      trie("a") must be("a")
      trie("x") must be("*")
      trie("xx") must be("*")
      trie("ab") must be("ab")
      trie("ax") must be("a*")
      trie("axx") must be("a*")
      trie("abc") must be("abc")
      trie("abx") must be("ab*")
      trie("abxx") must be("ab*")
      trie("b") must be("b*")
    }

    "collect all wildly at end" in {
      val trie = makeTrie("a", "*", "ab", "a*", "b*")

      trie.getAll("a") must be(Set("a", "*"))
      trie.getAll("x") must be(Set("*"))
      trie.getAll("xx") must be(Set("*"))
      trie.getAll("ab") must be(Set("*", "ab", "a*"))
      trie.getAll("ax") must be(Set("*", "a*"))
      trie.getAll("axx") must be(Set("*", "a*"))
      trie.getAll("b") must be(Set("*", "b*"))
    }

    "match wildly in middle" in {
      val trie = makeTrie("a*b")

      trie("ab") must be("a*b")
      trie("axb") must be("a*b")
      trie("axxb") must be("a*b")
    }

    "collect all wildly in middle" in {
      val trie = makeTrie("a*bc", "ab*c", "a*b*c")

      trie.getAll("abc") must be(Set("a*bc", "ab*c", "a*b*c"))
      trie.getAll("axbc") must be(Set("a*bc", "a*b*c"))
      trie.getAll("axxbc") must be(Set("a*bc", "a*b*c"))
      trie.getAll("abxc") must be(Set("ab*c", "a*b*c"))
      trie.getAll("abxxc") must be(Set("ab*c", "a*b*c"))
      trie.getAll("axbxc") must be(Set("a*b*c"))
      trie.getAll("axxbxxc") must be(Set("a*b*c"))
    }

    "match wildly anywhere" in {
      val trie = makeTrie("a", "*", "ab", "a*", "*b", "abc", "ab*", "a*c", "*bc", "b*", "*c")

      trie("a") must be("a")
      trie("x") must be("*")
      trie("xx") must be("*")
      trie("xa") must be("*")
      trie("ab") must be("ab")
      trie("ax") must be("a*")
      trie("axx") must be("a*")
      trie("xb") must be("*b")
      trie("xxb") must be("*b")
      trie("abc") must be("abc")
      trie("abx") must be("ab*")
      trie("abxx") must be("ab*")
      trie("axc") must be("a*c")
      trie("axxc") must be("a*c")
      trie("xbc") must be("*bc")
      trie("xxbc") must be("*bc")
      trie("b") must be("b*")
      trie("bc") must be("b*")
      trie("ac") must be("a*c")
      trie("xc") must be("*c")
    }

    "collect all wildly anywhere" in {
      val trie = makeTrie("a", "*", "ab", "a*", "*b")

      trie.getAll("a") must be(Set("a", "*"))
      trie.getAll("x") must be(Set("*"))
      trie.getAll("xx") must be(Set("*"))
      trie.getAll("ab") must be(Set("*", "ab", "a*", "*b"))
      trie.getAll("ax") must be(Set("*", "a*"))
      trie.getAll("axx") must be(Set("*", "a*"))
      trie.getAll("xb") must be(Set("*", "*b"))
      trie.getAll("xxb") must be(Set("*", "*b"))
      trie.getAll("xbx") must be(Set("*"))
    }

    "escape wildcard" in {
      val trie = makeTrie("a*", "a\\*", "b*", """b\*""")

      trie("a") must be("a*")
      trie("ax") must be("a*")
      trie("a*") must be("a\\*")
      trie("a*x") must be("a*")
      trie("b*") must be("b\\*")
    }
  }
}
