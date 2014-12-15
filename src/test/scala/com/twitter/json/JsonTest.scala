/*
 * Copyright 2009-2014 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.json

import com.twitter.json.extensions._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class JsonTest extends FunSuite {
  test("Json should quote strings for unicode within latin-1") {
    assert(Json.quote("hello\n\u009f") === "\"hello\\n\\u009f\"")
  }

  test("Json should quote strings for unicode outside of latin-1 (the word Tokyo)") {
    assert(Json.quote("\u6771\u4eac") === "\"\\u6771\\u4eac\"")
  }

  test("Json should escape control characters") {
    val controlChars = "\u0000\u001f\u0020"
    assert(Json.quote(controlChars) === "\"\\u0000\\u001f\u0020\"")
  }

  test("Json should quote string containing unicode outside of the BMP " +
    "(using UTF-16 surrogate pairs)") {
    val ridiculous = new java.lang.StringBuilder()
    ridiculous.appendCodePoint(0xfe03e)
    assert(Json.quote(ridiculous.toString) === "\"\\udbb8\\udc3e\"")
  }

  test("Json should quote xml") {
    assert(Json.quote("<xml>sucks</xml>") === "\"<xml>sucks<\\/xml>\"")
  }

  test ("Json should encode nested objects") {
    assert(Json.build(Json.build(List(1, 2))).toString === "[1,2]")
        // If this triggers, it means you are excessively escaping, sucker.
    assert(Json.build(Json.build(List(1, 2))).toString !== "\"[1,2]\"")
  }

  test("Json should parse strings with double slashes like one finds in URLs") {
    assert(Json.parse("""["hey! http:\/\/www.lollerskates.com"]""").asInstanceOf[List[String]] ===
      List("hey! http://www.lollerskates.com"))
  }

  test("Json should parse strings with quoted newlines") {
    assert(Json.parse("""["hi\njerk"]""").asInstanceOf[List[String]] === List("hi\njerk"))
  }

  test("Json should parse empty strings") {
    assert(Json.parse("""[""]""").asInstanceOf[List[String]] === List(""))
  }

  test("Json should parse strings with quoted quotes") {
    assert(Json.parse("""["x\"x"]""").asInstanceOf[List[String]] === List("x\"x"))
  }

  test("Json should accept unquoted DEL char, as isn't considered control char in Json spec") {
    assert(Json.parse("[\"A\u007fB\"]").asInstanceOf[List[String]] === List("A\u007fB"))
  }

  test("Json should parse escaped string thing followed by whitespace") {
    assert(Json.parse("[\"\\u2603  q\"]").asInstanceOf[List[String]] === List("\u2603  q"))
    assert(Json.parse("[\"\\t q\"]").asInstanceOf[List[String]] === List("\t q"))
  }

  test("Json should parse unicode outside of the BMP") {
    assert(Json.parse("[\"\\udbb8\\udc3e\"]").asInstanceOf[List[String]] ===
      List(new String(Character.toChars(0x0FE03E))))
  }

  test("Json parsing should not strip leading whitespace") {
    assert(Json.parse("""[" f"]""").asInstanceOf[List[String]] === List(" f"))
  }

  test("Json should parse escaped backspace at end of string") {
    assert(Json.parse("""["\\", "\\"]""").asInstanceOf[List[String]] === List("""\""", """\"""))
  }

  test("Json should parse long strings") {
    val longString = "{ \"long string\":\"" +
      (1 to 1000).map(x => "That will be a long string").mkString + "\"}  "
    assert(Json.parse(longString).asInstanceOf[Map[String, String]].size === 1)
  }

  test("Json should parse floating point numbers") {
    assert(Json.parse("[1.42]").asInstanceOf[List[BigDecimal]] === List(BigDecimal("1.42")))
  }

  test("Json should parse floating point numbers with exponents") {
    assert(Json.parse("[1.42e10]").asInstanceOf[List[BigDecimal]] === List(BigDecimal("1.42e10")))
  }

  test("Json should parse integers with exponents") {
    assert(Json.parse("[42e10]").asInstanceOf[List[BigDecimal]] === List(BigDecimal("42e10")))
  }

  test("Json should parse integer numbers") {
    assert(Json.parse("[42]").asInstanceOf[List[Int]] === List(42))
  }

  test("Json should parse empty maps") {
    assert(Json.parse("{}").asInstanceOf[Map[String, Any]] === Map.empty[String, Any])
  }

  test("Json should parse maps of empty lists") {
    assert(Json.parse("{\"nil\":[]}").asInstanceOf[Map[String, List[Any]]] ===
      Map("nil" -> List.empty[Any]))
  }

  test("Json should parse empty maps as values") {
    assert(Json.parse("{\"empty\":{}}").asInstanceOf[Map[String, Map[String, Any]]] ===
      Map("empty" -> Map.empty[String, Any]))
  }

  test("Json should parse simple maps") {
    val jsonString = "{\"user_id\": 1554, \"message\": \"your phone is being turned off.\"}"

    assert(Json.parse(jsonString).asInstanceOf[Map[String, Any]] ===
      Map("user_id" -> 1554, "message" -> "your phone is being turned off."))
  }

  test("Json should parse simple maps with longs") {
    val jsonString = "{\"user_id\": 1554, \"status_id\": 9015551486 }"

    assert(Json.parse(jsonString).asInstanceOf[Map[String, Any]] ===
      Map("user_id" -> 1554, "status_id" -> 9015551486L))
  }

  test("Json should parse map with map") {
    val jsonString = "{\"name\":\"nathaniel\",\"status\":{\"text\":\"i like to dance!\"," +
      "\"created_at\":666},\"zipcode\":94103}"

    assert(Json.parse(jsonString).asInstanceOf[Map[String, Any]] ===
      Map("name" -> "nathaniel",
          "status" -> Map("text" -> "i like to dance!",
                          "created_at" -> 666),
          "zipcode" -> 94103))
  }

  test("Json should parse maps with lists") {
    val jsonString = "{\"names\":[\"nathaniel\",\"brittney\"]}"

    assert(Json.parse(jsonString).asInstanceOf[Map[String, List[String]]] ===
      Map("names" -> List("nathaniel", "brittney")))
  }

  test("Json should parse maps with two lists") {
    val jsonString = "{\"names\":[\"nathaniel\",\"brittney\"],\"ages\":[4,7]}"

    assert(Json.parse(jsonString).asInstanceOf[Map[String, Any]] ===
      Map("names" -> List("nathaniel", "brittney"),
          "ages" -> List(4, 7)))
  }

  test("Json should parse maps with lists, booleans, and maps") {
    val jsonString = "{\"names\":[\"nathaniel\",\"brittney\"],\"adults\":false," +
      "\"ages\":{\"nathaniel\":4,\"brittney\":7}}"

    assert(Json.parse(jsonString).asInstanceOf[Map[String, Any]] ===
      Map("names" -> List("nathaniel", "brittney"),
          "adults" -> false,
          "ages" -> Map("nathaniel" -> 4,
                        "brittney" -> 7)))
  }

  test("Json should build empty maps") {
    assert(Json.build(Map()).toString === "{}")
  }

  test("Json should build empty lists") {
    assert(Json.build(Map("nil" -> Nil)).toString === "{\"nil\":[]}")
  }

  test("Json should build empty maps as values") {
    assert(Json.build(Map("empty" -> Map())).toString === "{\"empty\":{}}")
  }

  test("Json should build simple maps") {
    assert(Json.build(Map("name" -> "nathaniel",
                          "likes" -> "to dance",
                          "age" -> 4)).toString ===
      "{\"age\":4,\"likes\":\"to dance\",\"name\":\"nathaniel\"}")
  }

  test("Json should build simple lists of integers") {
    assert(Json.build(List(1, 2, 3)).toString === "[1,2,3]")
  }

  test("Json should build simple maps with longs") {
    assert(Json.build(Map("user_id" -> 1554, "status_id" -> 9015551486L)).toString ===
      "{\"status_id\":9015551486,\"user_id\":1554}")
  }

  test("Json should build maps with nested maps") {
    assert(Json.build(Map("name" -> "nathaniel",
                          "status" -> Map("text" -> "i like to dance!",
                                          "created_at" -> 666),
                          "zipcode" -> 94103)).toString ===
      "{\"name\":\"nathaniel\",\"status\":{\"created_at\":666,\"text\":\"i like to dance!\"}," +
        "\"zipcode\":94103}")
  }

  test("Json should build nested immutable maps") {
    assert(Json.build(Map("name" -> "nathaniel",
                          "status" -> Map("created_at" -> 666, "text" -> "i like to dance!"),
                          "zipcode" -> 94103)).toString ===
      "{\"name\":\"nathaniel\",\"status\":{\"created_at\":666,\"text\":\"i like to dance!\"}," +
        "\"zipcode\":94103}")
  }

  test("Json should build appended immutable maps") {
    val statusMap = Map("status" -> Map("text" -> "i like to dance!", "created_at" -> 666))

    assert(Json.build(Map.empty ++
                      Map("name" -> "nathaniel") ++
                      statusMap ++
                      Map("zipcode" -> 94103)).toString ===
      "{\"name\":\"nathaniel\",\"status\":{\"created_at\":666,\"text\":\"i like to dance!\"}," +
        "\"zipcode\":94103}")
  }

  test("Json should build nested, literal mutable maps") {
    val map = mutable.Map("name" -> "nathaniel",
                          "status" -> mutable.Map("text" -> "i like to dance!",
                                                  "created_at" -> 666),
                          "zipcode" -> 94103)

    assert(Json.build(map).toString ===
      """{"name":"nathaniel","status":{"created_at":666,"text":"i like to dance!"},""" +
        """"zipcode":94103}""")
  }

  test("Json should build nested, appended mutable maps") {
    val statusMap = mutable.Map("status" -> Map("text" -> "i like to dance!",
                                                "created_at" -> 666))

    val nestedMap = mutable.Map[String, Any]() ++
                    mutable.Map("name" -> "nathaniel") ++
                    statusMap ++
                    mutable.Map("zipcode" -> 94103)

    assert(Json.build(nestedMap).toString ===
      """{"name":"nathaniel","status":{"created_at":666,"text":"i like to dance!"},""" +
        """"zipcode":94103}""")
  }

  test("Json should build maps with lists") {
    assert(Json.build(Map("names" -> List("nathaniel", "brittney"))).toString ===
      "{\"names\":[\"nathaniel\",\"brittney\"]}")
  }

  test("Json should build maps with two lists") {
    assert(Json.build(Map("names" -> List("nathaniel", "brittney"),
                          "ages" -> List(4, 7))).toString ===
      "{\"ages\":[4,7],\"names\":[\"nathaniel\",\"brittney\"]}")
  }

  test("Json should build maps with lists, booleans, and maps") {
    assert(Json.build(Map("names" -> List("nathaniel", "brittney"),
                          "adults" -> false,
                          "ages" -> Map("nathaniel" -> 4,
                                        "brittney" -> 7))).toString ===
      "{\"adults\":false," +
        "\"ages\":{\"brittney\":7,\"nathaniel\":4}," +
        "\"names\":[\"nathaniel\",\"brittney\"]}")
  }

  test("Json should parse empty lists") {
    assert(Json.parse("[]").asInstanceOf[Nil.type] === Nil)
  }

  test("Json should parse lists of empty lists") {
    assert(Json.parse("[[]]").asInstanceOf[List[Nil.type]] === List(Nil))
  }

  test("Json should parse lists with empty maps") {
    assert(Json.parse("[{}]").asInstanceOf[List[Map[String, Any]]] === List(Map.empty[String, Any]))
  }

  test("Json should parse simple lists") {
    assert(Json.parse("[\"id\", 1]").asInstanceOf[List[Any]] === List("id", 1))
  }

  test("Json should parse nested lists") {
    assert(Json.parse("[\"more lists!\",[1,2,\"three\"]]").asInstanceOf[List[Any]] ===
      List("more lists!", List(1, 2, "three")))
  }

  test("Json should parse lists with maps") {
    assert(Json.parse("[\"maptastic!\",{\"1\":2}]").asInstanceOf[List[Any]] ===
      List("maptastic!", Map("1" -> 2)))
  }

  test("Json should parse lists with two maps") {
    assert(Json.parse("[{\"1\":2},{\"3\":4}]").asInstanceOf[List[Map[String, Int]]] ===
      List(Map("1" -> 2), Map("3" -> 4)))
  }

  test("Json should parse lists with lists, booleans, and maps") {
    val jsonString = "{\"names\":[\"nathaniel\",\"brittney\"],\"adults\":false," +
      "\"ages\":{\"nathaniel\":4,\"brittney\":7}}"

    assert(Json.parse(jsonString).asInstanceOf[Map[String, Any]] ===
      Map("names" -> List("nathaniel", "brittney"),
          "adults" -> false,
          "ages" -> Map("nathaniel" -> 4,
                        "brittney" -> 7)))
  }

  test("Json should parse lists with maps containing lists") {
    assert(Json.parse("[{\"1\":[2,3]}]").asInstanceOf[List[Map[String, Int]]] ===
      List(Map("1" -> List(2, 3))))
  }

  test("Json should parse lists with maps containing maps") {
    val jsonString = "[{\"1\":{\"2\":\"3\"}}]"

    assert(Json.parse(jsonString).asInstanceOf[List[Map[String, Map[String, String]]]] ===
      List(Map("1" -> Map("2" -> "3"))))
  }

  test("Json should parse nested maps and lists with lists in the middle") {
    val jsonString = """{"JobWithTasks":{"tasks":[{"Add":{"updated_at":12,"position":13}}],""" +
      """"error_count":1}}"""

    assert(Json.parse(jsonString).asInstanceOf[Map[String, Map[String, Any]]] ===
      Map("JobWithTasks" -> Map("tasks" -> List(Map("Add" -> Map("updated_at" -> 12,
                                                                 "position" -> 13))),
                                "error_count" -> 1)))
  }

  test("Json should build lists of empty lists") {
    assert(Json.build(List(Nil)).toString === "[[]]")
  }

  test("Json should build lists of empty maps") {
    assert(Json.build(List(Map())).toString === "[{}]")
  }

  test("Json should build simple lists") {
    assert(Json.build(List("id", 1)).toString === "[\"id\",1]")
  }

  test("Json should build nested lists") {
    assert(Json.build(List("more lists!", List(1, 2, "three"))).toString ===
      "[\"more lists!\",[1,2,\"three\"]]")
  }

  test("Json should build lists with maps") {
    assert(Json.build(List("maptastic!", Map("1" -> 2))).toString === "[\"maptastic!\",{\"1\":2}]")
  }

  test("Json should build lists with two maps") {
    assert(Json.build(List(Map("1" -> 2), Map("3" -> 4))).toString === "[{\"1\":2},{\"3\":4}]")
  }

  test("Json should build lists with maps containing lists") {
    assert(Json.build(List(Map("1" -> List(2, 3)))).toString === "[{\"1\":[2,3]}]")
  }

  test("Json should build lists with maps containing maps") {
    assert(Json.build(List(Map("1" -> Map("2" -> "3")))).toString ===
      "[{\"1\":{\"2\":\"3\"}}]")
  }

  test("Json should build numbers") {
    assert(
      Json.build(List(42, 23L, 1.67, BigDecimal("1.67456352431287348917591342E+50"))).toString ===
        "[42,23,1.67,1.67456352431287348917591342E+50]")
  }

  test("Json should build floating point numbers") {
    assert(Json.build(List(0.0, 5.25)).toString === "[0.0,5.25]")
  }

  test("Json should build simple arrays") {
    assert(Json.build(Array(0, 1)).toString === "[0,1]")
  }

  test("Json should build nested, mixed arrays") {
    assert(Json.build(Array(Array(0, 1), 2.asInstanceOf[AnyRef])).toString === "[[0,1],2]")
  }

  test("Json should build nested arrays") {
    assert(Json.build(Array(Array(0, 1), Array(2, 3))).toString === "[[0,1],[2,3]]")
  }

  test("Json should build arrays inside of lists") {
    assert(Json.build(List(Array(0, 1))).toString === "[[0,1]]")
    assert(Json.build(List(Array(0, 1), Array(2, 3))).toString === "[[0,1],[2,3]]")
  }

  test("Json should build maps containing arrays") {
    assert(Json.build(List(Map("1" -> Array(0, 2)))).toString === "[{\"1\":[0,2]}]")
  }

  test("Json should build arrays containing maps") {
    assert(Json.build(Array(Map("1" -> 2))).toString === "[{\"1\":2}]")
  }

  test("Json should build JsonSerializable objects") {
    val obj = new JsonSerializable {
      def toJson() = "\"abracadabra\""
    }

    assert(Json.build(List(obj, 23)).toString === "[\"abracadabra\",23]")
  }
}
