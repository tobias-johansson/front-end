/*
 * Copyright (c) Neo4j Sweden AB (http://neo4j.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.v9_0.parser

import org.opencypher.v9_0.expressions
import org.opencypher.v9_0.expressions.ExtractScope
import org.opencypher.v9_0.expressions.GreaterThan
import org.opencypher.v9_0.expressions.ListComprehension
import org.opencypher.v9_0.expressions.Property
import org.opencypher.v9_0.expressions.SignedDecimalIntegerLiteral
import org.opencypher.v9_0.util.DummyPosition
import org.parboiled.scala.EOI
import org.parboiled.scala.Rule1

class ListComprehensionTest extends ParserTest[expressions.ListComprehension, Any] with Expressions {
  implicit val parserToTest: Rule1[ListComprehension] = ListComprehension ~ EOI
  val t = DummyPosition(0)

  test("tests") {

    parsing("[ a in p WHERE a.foo > 123 ]") shouldGive
      expressions.ListComprehension(ExtractScope(expressions.Variable("a")(t),
                                         Some(GreaterThan(
                                           Property(expressions.Variable("a")(t),
                                                    expressions.PropertyKeyName("foo")(t))(t),
                                           SignedDecimalIntegerLiteral("123")(t))(t)),
                                         None)(t),
                            expressions.Variable("p")(t))(t)

    parsing("[ a in p | a.foo ]") shouldGive
      expressions.ListComprehension(ExtractScope(expressions.Variable("a")(t),
                                         None,
                                         Some(Property(expressions.Variable("a")(t),expressions.PropertyKeyName("foo")(t))(t))
                                        )(t),
                            expressions.Variable("p")(t))(t)

    parsing("[ a in p WHERE a.foo > 123 | a.foo ]") shouldGive
      expressions.ListComprehension(ExtractScope(expressions.Variable("a")(t),
                                         Some(GreaterThan(
                                           Property(expressions.Variable("a")(t),
                                                    expressions.PropertyKeyName("foo")(t))(t),
                                           SignedDecimalIntegerLiteral("123")(t))(t)),
                                         Some(Property(expressions.Variable("a")(t),expressions.PropertyKeyName("foo")(t))(t))
                                        )(t),
                            expressions.Variable("p")(t))(t)
  }

  def convert(result: expressions.ListComprehension): Any = result
}
