/*
 * Copyright © 2002-2020 Neo4j Sweden AB (http://neo4j.com)
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

import java.util

import org.opencypher.v9_0.ast
import org.opencypher.v9_0.ast.AliasedReturnItem
import org.opencypher.v9_0.ast.UnaliasedReturnItem
import org.opencypher.v9_0.expressions.SensitiveParameter
import org.opencypher.v9_0.expressions.SensitiveStringLiteral

class UserAdministrationCommandParserTest extends AdministrationCommandParserTestBase {
  private val varUser = varFor("user")
  private val password = pw("password")
  private val passwordNew = pw("new")
  private val passwordCurrent = pw("current")
  private val passwordEmpty = pw("")
  private val paramPassword = pwParam("password")
  private val paramPasswordNew = pwParam("newPassword")
  private val paramPasswordCurrent = pwParam("currentPassword")

  //  Showing user

  test("SHOW USERS") {
    yields(ast.ShowUsers(None, None))
  }

  test("CATALOG SHOW USERS") {
    yields(ast.ShowUsers(None, None))
  }

  test("CATALOG SHOW USER") {
    failsToParse
  }

  test("SHOW USERS WHERE user = 'GRANTED'") {
    yields(ast.ShowUsers(Some(Right(ast.Where(equals(varUser, grantedString)) _)), None))
  }

  test("SHOW USERS WHERE user = 'GRANTED' AND action = 'match'") {
    val accessPredicate = equals(varUser, grantedString)
    val matchPredicate = equals(varFor("action"), literalString("match"))
    yields(ast.ShowUsers(Some(Right(ast.Where(and(accessPredicate, matchPredicate)) _)), None))
  }

  test("SHOW USERS YIELD user ORDER BY user") {
    val orderBy = ast.OrderBy(List(ast.AscSortItem(varUser) _)) _
    val columns = ast.Yield(ast.ReturnItems(includeExisting = false, List(UnaliasedReturnItem(varUser, "user") _)) _, Some(orderBy), None, None, None) _
    yields(ast.ShowUsers(Some(Left(columns)), None))
  }

  test("SHOW USERS YIELD user ORDER BY user WHERE user ='none'") {
    val orderBy = ast.OrderBy(List(ast.AscSortItem(varUser) _)) _
    val where = ast.Where(equals(varUser, literalString("none"))) _
    val columns = ast.Yield(ast.ReturnItems(includeExisting = false, List(UnaliasedReturnItem(varUser, "user") _)) _, Some(orderBy), None, None, Some(where)) _
    yields(ast.ShowUsers(Some(Left(columns)) , None))
  }

  test("SHOW USERS YIELD user ORDER BY user SKIP 1 LIMIT 10 WHERE user ='none'") {
    val orderBy = ast.OrderBy(List(ast.AscSortItem(varUser) _)) _
    val where = ast.Where(equals(varUser, literalString("none"))) _
    val columns = ast.Yield(ast.ReturnItems(includeExisting = false, List(UnaliasedReturnItem(varUser, "user") _)) _, Some(orderBy),
      Some(ast.Skip(literalInt(1)) _), Some(ast.Limit(literalInt(10)) _), Some(where)) _
    yields(ast.ShowUsers(Some(Left(columns)), None))
  }

  test("SHOW USERS YIELD user SKIP -1") {
    val columns = ast.Yield(ast.ReturnItems(includeExisting = false, List(UnaliasedReturnItem(varUser, "user") _)) _, None,
      Some(ast.Skip(literalInt(-1)) _), None, None) _
    yields(ast.ShowUsers(Some(Left(columns)), None))
  }

  test("SHOW USERS YIELD user RETURN user ORDER BY user") {
    yields(ast.ShowUsers(
      Some(Left(ast.Yield(ast.ReturnItems(includeExisting = false, List(UnaliasedReturnItem(varUser, "user") _)) _, None, None, None, None) _)),
      Some(ast.Return(distinct = false, ast.ReturnItems(includeExisting = false, List(UnaliasedReturnItem(varUser, "user") _)) _, Some(ast.OrderBy(List(ast.AscSortItem(varUser) _)) _), None, None) _)
    ))
  }

  test("SHOW USERS YIELD user, suspended as suspended WHERE suspended RETURN DISTINCT user") {
    yields(ast.ShowUsers(
      Some(Left(ast.Yield(ast.ReturnItems(includeExisting = false, List(UnaliasedReturnItem(varUser, "user") _, AliasedReturnItem(varFor("suspended")))) _, None, None, None,
        Some(ast.Where(varFor("suspended")) _)) _)),
      Some(ast.Return(distinct = true, ast.ReturnItems(includeExisting = false, List(UnaliasedReturnItem(varUser, "user") _)) _, None, None, None) _)
    ))
  }

  test("SHOW USERS YIELD * RETURN *") {
    yields(ast.ShowUsers(
      Some(Left(ast.Yield(ast.ReturnItems(includeExisting = true, List()) _, None, None, None, None) _)),
      Some(ast.Return(distinct = false, ast.ReturnItems(includeExisting = true, List()) _, None, None, None, Set()) _)
    ))
  }

  test("SHOW USERS YIELD *,blah RETURN user") {
    failsToParse
  }

  //  Creating user

  test("CATALOG CREATE USER foo SET PASSWORD 'password'") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CATALOG CREATE USER $foo SET PASSWORD 'password'") {
    yields(ast.CreateUser(paramFoo, password, requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CATALOG CREATE USER $bar SET PASSWORD $pw") {
    yields(ast.CreateUser(param("bar"), pwParam("pw"), requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CREATE USER `foo` SET PASSwORD 'password'") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CREATE USER `!#\"~` SeT PASSWORD 'password'") {
    yields(ast.CreateUser(literal("!#\"~"), password, requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CREATE USER foo SeT PASSWORD 'pasS5Wor%d'") {
    yields(ast.CreateUser(literalFoo, pw("pasS5Wor%d"), requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CREATE USER foo SET PASSwORD ''") {
    yields(ast.CreateUser(literalFoo, passwordEmpty, requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CREATE uSER foo SET PASSWORD $password") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CREaTE USER foo SET PASSWORD 'password' CHANGE REQUIRED") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CATALOG CREATE USER foo SET PASSWORD $password CHANGE REQUIRED") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CREATE USER foo SET PASSWORD 'password' SET PASSWORD CHANGE required") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CREATE USER foo SET PASSWORD 'password' CHAngE NOT REQUIRED") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = false, suspended = None, ast.IfExistsThrowError))
  }

  test("CREATE USER foo SET PASSWORD 'password' SET PASSWORD CHANGE NOT REQUIRED") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = false, suspended = None, ast.IfExistsThrowError))
  }

  test("CREATE USER foo SET PASSWORD $password SET  PASSWORD CHANGE NOT REQUIRED") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = false, suspended = None, ast.IfExistsThrowError))
  }

  test("CATALOG CREATE USER foo SET PASSWORD 'password' SET STATUS SUSPENDed") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = true, suspended = Some(true), ast.IfExistsThrowError))
  }

  test("CREATE USER foo SET PASSWORD 'password' SET STATUS ACtiVE") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = true, suspended = Some(false), ast.IfExistsThrowError))
  }

  test("CREATE USER foo SET PASSWORD 'password' SET PASSWORD CHANGE NOT REQUIRED SET   STATuS SUSPENDED") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = false, suspended = Some(true), ast.IfExistsThrowError))
  }

  test("CREATE USER foo SET PASSWORD $password CHANGE REQUIRED SET STATUS SUSPENDED") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = true, suspended = Some(true), ast.IfExistsThrowError))
  }

  test("CREATE USER `` SET PASSwORD 'password'") {
    yields(ast.CreateUser(literalEmpty, password, requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CREATE USER `f:oo` SET PASSWORD 'password'") {
    yields(ast.CreateUser(literalFColonOo, password, requirePasswordChange = true, suspended = None, ast.IfExistsThrowError))
  }

  test("CATALOG CREATE USER foo IF NOT EXISTS SET PASSWORD 'password'") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = true, suspended = None, ast.IfExistsDoNothing))
  }

  test("CREATE uSER foo IF NOT EXISTS SET PASSWORD $password") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = true, suspended = None, ast.IfExistsDoNothing))
  }

  test("CATALOG CREATE USER foo IF NOT EXISTS SET PASSWORD $password CHANGE REQUIRED") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = true, suspended = None, ast.IfExistsDoNothing))
  }

  test("CREATE USER foo IF NOT EXISTS SET PASSWORD $password SET STATUS SUSPENDED") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = true, suspended = Some(true), ast.IfExistsDoNothing))
  }

  test("CREATE USER foo IF NOT EXISTS SET PASSWORD $password CHANGE REQUIRED SET STATUS SUSPENDED") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = true, suspended = Some(true), ast.IfExistsDoNothing))
  }

  test("CATALOG CREATE OR REPLACE USER foo SET PASSWORD 'password'") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = true, suspended = None, ast.IfExistsReplace))
  }

  test("CREATE OR REPLACE uSER foo SET PASSWORD $password") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = true, suspended = None, ast.IfExistsReplace))
  }

  test("CATALOG CREATE OR REPLACE USER foo SET PASSWORD $password CHANGE REQUIRED") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = true, suspended = None, ast.IfExistsReplace))
  }

  test("CREATE OR REPLACE USER foo SET PASSWORD $password SET STATUS SUSPENDED") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = true, suspended = Some(true), ast.IfExistsReplace))
  }

  test("CREATE OR REPLACE USER foo SET PASSWORD $password CHANGE REQUIRED SET STATUS SUSPENDED") {
    yields(ast.CreateUser(literalFoo, paramPassword, requirePasswordChange = true, suspended = Some(true), ast.IfExistsReplace))
  }

  test("CREATE OR REPLACE USER foo IF NOT EXISTS SET PASSWORD 'password'") {
    yields(ast.CreateUser(literalFoo, password, requirePasswordChange = true, suspended = None, ast.IfExistsInvalidSyntax))
  }

  test("CREATE USER foo") {
    failsToParse
  }

  test("CREATE USER \"foo\" SET PASSwORD 'password'") {
    failsToParse
  }

  test("CREATE USER !#\"~ SeT PASSWORD 'password'") {
    failsToParse
  }

  test("CATALOG CREATE USER fo,o SET PASSWORD 'password'") {
    failsToParse
  }

  test("CREATE USER f:oo SET PASSWORD 'password'") {
    failsToParse
  }

  test("CREATE USER foo SET PASSWORD") {
    failsToParse
  }

  test("CREATE USER foo SET PASSwORD 'passwordString'+$passwordexpressions.Parameter") {
    failsToParse
  }

  test("CREATE USER foo SET PASSWORD null CHANGE REQUIRED") {
    failsToParse
  }

  test("CREATE USER foo PASSWORD 'password'") {
    failsToParse
  }

  test("CREATE USER foo SET PASSWORD 'password' SET STAUS ACTIVE") {
    failsToParse
  }

  test("CREATE USER foo SET PASSWORD 'password' SET STATUS IMAGINARY") {
    failsToParse
  }

  test("CREATE USER foo SET PASSWORD 'password' SET STATUS") {
    failsToParse
  }

  test("CREATE USER foo SET PASSWORD CHANGE REQUIRED") {
    failsToParse
  }

  test("CREATE USER foo SET STATUS SUSPENDED") {
    failsToParse
  }

  test("CREATE USER foo SET PASSWORD CHANGE REQUIRED SET STATUS ACTIVE") {
    failsToParse
  }

  test( "CREATE USER foo IF EXISTS SET PASSWORD 'bar'") {
    failsToParse
  }

  test("CREATE USER foo IF NOT EXISTS") {
    failsToParse
  }

  test("CREATE USER foo IF NOT EXISTS SET PASSWORD") {
    failsToParse
  }

  test("CREATE USER foo IF NOT EXISTS SET PASSWORD CHANGE REQUIRED") {
    failsToParse
  }

  test("CREATE USER foo IF NOT EXISTS SET STATUS ACTIVE") {
    failsToParse
  }

  test("CREATE USER foo IF NOT EXISTS SET PASSWORD CHANGE NOT REQUIRED SET STATUS SUSPENDED") {
    failsToParse
  }

  test("CREATE OR REPLACE USER foo") {
    failsToParse
  }

  test("CREATE OR REPLACE USER foo SET PASSWORD") {
    failsToParse
  }

  test("CREATE OR REPLACE USER foo SET PASSWORD CHANGE NOT REQUIRED") {
    failsToParse
  }

  test("CREATE OR REPLACE USER foo SET STATUS SUSPENDED") {
    failsToParse
  }

  test("CREATE OR REPLACE USER foo SET PASSWORD CHANGE REQUIRED SET STATUS ACTIVE") {
    failsToParse
  }

  test("CREATE command finds password literal at correct offset") {
    parsing("CREATE USER foo SET PASSWORD 'password'").shouldVerify { statement =>
      val passwords = statement.findByAllClass[SensitiveStringLiteral].map(l => (l.value, l.position.offset))
      passwords.foreach { case (pw, offset) =>
        withClue("Expecting password = password, offset = 29") {
          util.Arrays.equals(toUtf8Bytes("password"), pw) shouldBe true
          offset shouldBe 29
        }
      }
    }
  }

  test("CREATE command finds password parameter at correct offset") {
    parsing("CREATE USER foo SET PASSWORD $param").shouldVerify { statement =>
      val passwords = statement.findByAllClass[SensitiveParameter].map(p => (p.name, p.position.offset))
      passwords should equal(Seq("param" -> 29))
    }
  }

  //  Dropping user

  test("DROP USER foo") {
    yields(ast.DropUser(literalFoo, ifExists = false))
  }

  test("DROP USER $foo") {
    yields(ast.DropUser(paramFoo, ifExists = false))
  }

  test("DROP USER ``") {
    yields(ast.DropUser(literalEmpty, ifExists = false))
  }

  test("DROP USER `f:oo`") {
    yields(ast.DropUser(literalFColonOo, ifExists = false))
  }

  test("DROP USER foo IF EXISTS") {
    yields(ast.DropUser(literalFoo, ifExists = true))
  }

  test("DROP USER `` IF EXISTS") {
    yields(ast.DropUser(literalEmpty, ifExists = true))
  }

  test("DROP USER `f:oo` IF EXISTS") {
    yields(ast.DropUser(literalFColonOo, ifExists = true))
  }

  test("DROP USER ") {
    failsToParse
  }

  test("DROP USER  IF EXISTS") {
    failsToParse
  }

  test("DROP USER foo IF NOT EXISTS") {
    failsToParse
  }

  //  Altering user

  test("CATALOG ALTER USER foo SET PASSWORD 'password'") {
    yields(ast.AlterUser(literalFoo, Some(password), None, None))
  }

  test("CATALOG ALTER USER $foo SET PASSWORD 'password'") {
    yields(ast.AlterUser(paramFoo, Some(password), None, None))
  }

  test("ALTER USER `` SET PASSWORD 'password'") {
    yields(ast.AlterUser(literalEmpty, Some(password), None, None))
  }

  test("ALTER USER `f:oo` SET PASSWORD 'password'") {
    yields(ast.AlterUser(literalFColonOo, Some(password), None, None))
  }

  test("ALTER USER foo SET PASSWORD ''") {
    yields(ast.AlterUser(literalFoo, Some(passwordEmpty), None, None))
  }

  test("ALTER USER foo SET PASSWORD $password") {
    yields(ast.AlterUser(literalFoo, Some(paramPassword), None, None))
  }

  test("CATALOG ALTER USER foo SET PASSWORD CHANGE REQUIRED") {
    yields(ast.AlterUser(literalFoo, None, requirePasswordChange = Some(true), None))
  }

  test("CATALOG ALTER USER foo SET PASSWORD CHANGE NOT REQUIRED") {
    yields(ast.AlterUser(literalFoo, None, requirePasswordChange = Some(false), None))
  }

  test("ALTER USER foo SET STATUS SUSPENDED") {
    yields(ast.AlterUser(literalFoo, None, None, suspended = Some(true)))
  }

  test("ALTER USER foo SET STATUS ACTIVE") {
    yields(ast.AlterUser(literalFoo, None, None, suspended = Some(false)))
  }

  test("CATALOG ALTER USER foo SET PASSWORD 'password' CHANGE REQUIRED") {
    yields(ast.AlterUser(literalFoo, Some(password), requirePasswordChange = Some(true), None))
  }

  test("ALTER USER foo SET PASSWORD $password SET PASSWORD CHANGE NOT REQUIRED") {
    yields(ast.AlterUser(literalFoo, Some(paramPassword), requirePasswordChange = Some(false), None))
  }

  test("CATALOG ALTER USER foo SET PASSWORD 'password' SET STATUS ACTIVE") {
    yields(ast.AlterUser(literalFoo, Some(password), None, suspended = Some(false)))
  }

  test("CATALOG ALTER USER foo SET PASSWORD CHANGE NOT REQUIRED SET STATUS ACTIVE") {
    yields(ast.AlterUser(literalFoo, None, requirePasswordChange = Some(false), suspended = Some(false)))
  }

  test("ALTER USER foo SET PASSWORD $password SET PASSWORD CHANGE NOT REQUIRED SET STATUS SUSPENDED") {
    yields(ast.AlterUser(literalFoo, Some(paramPassword), requirePasswordChange = Some(false), suspended = Some(true)))
  }

  test("ALTER USER foo") {
    failsToParse
  }

  test("ALTER USER foo SET PASSWORD null") {
    failsToParse
  }

  test("ALTER USER foo SET PASSWORD 123") {
    failsToParse
  }

  test("ALTER USER foo SET PASSWORD") {
    failsToParse
  }

  test("ALTER USER foo SET STATUS") {
    failsToParse
  }

  test("ALTER USER foo SET PASSWORD 'password' SET PASSWORD SET STATUS ACTIVE") {
    failsToParse
  }

  test("ALTER USER foo SET PASSWORD STATUS ACTIVE") {
    failsToParse
  }

  test("CATALOG ALTER USER foo SET PASSWORD 'password' SET STATUS IMAGINARY") {
    failsToParse
  }

  test("ALTER user command finds password literal at correct offset") {
    parsing("ALTER USER foo SET PASSWORD 'password'").shouldVerify { statement =>
      val passwords = statement.findByAllClass[SensitiveStringLiteral].map(l => (l.value, l.position.offset))
      passwords.foreach { case (pw, offset) =>
        withClue("Expecting password = password, offset = 28") {
          util.Arrays.equals(toUtf8Bytes("password"), pw) shouldBe true
          offset shouldBe 28
        }
      }
    }
  }

  test("ALTER user command finds password parameter at correct offset") {
    parsing("ALTER USER foo SET PASSWORD $param").shouldVerify { statement =>
      val passwords = statement.findByAllClass[SensitiveParameter].map(p => (p.name, p.position.offset))
      passwords should equal(Seq("param" -> 28))
    }
  }

  // Changing own password

  test("ALTER CURRENT USER SET PASSWORD FROM 'current' TO 'new'") {
    yields(ast.SetOwnPassword(passwordNew, passwordCurrent))
  }

  test("alter current user set password from 'current' to ''") {
    yields(ast.SetOwnPassword(passwordEmpty, passwordCurrent))
  }

  test("alter current user set password from '' to 'new'") {
    yields(ast.SetOwnPassword(passwordNew, passwordEmpty))
  }

  test("ALTER CURRENT USER SET PASSWORD FROM 'current' TO 'passWORD123%!'") {
    yields(ast.SetOwnPassword(pw("passWORD123%!"), passwordCurrent))
  }

  test("ALTER CURRENT USER SET PASSWORD FROM 'current' TO $newPassword") {
    yields(ast.SetOwnPassword(paramPasswordNew, passwordCurrent))
  }

  test("ALTER CURRENT USER SET PASSWORD FROM $currentPassword TO 'new'") {
    yields(ast.SetOwnPassword(passwordNew, paramPasswordCurrent))
  }

  test("alter current user set password from $currentPassword to ''") {
    yields(ast.SetOwnPassword(passwordEmpty, paramPasswordCurrent))
  }

  test("ALTER CURRENT USER SET PASSWORD FROM $currentPassword TO 'passWORD123%!'") {
    yields(ast.SetOwnPassword(pw("passWORD123%!"), paramPasswordCurrent))
  }

  test("ALTER CURRENT USER SET PASSWORD FROM $currentPassword TO $newPassword") {
    yields(ast.SetOwnPassword(paramPasswordNew, paramPasswordCurrent))
  }

  test("ALTER CURRENT USER SET PASSWORD FROM 'current' TO null") {
    failsToParse
  }

  test("ALTER CURRENT USER SET PASSWORD FROM $current TO 123") {
    failsToParse
  }

  test("ALTER PASSWORD FROM 'current' TO 'new'") {
    failsToParse
  }

  test("ALTER CURRENT PASSWORD FROM 'current' TO 'new'") {
    failsToParse
  }

  test("ALTER CURRENT USER PASSWORD FROM 'current' TO 'new'") {
    failsToParse
  }

  test("ALTER CURRENT USER SET PASSWORD FROM 'current' TO") {
    failsToParse
  }

  test("ALTER CURRENT USER SET PASSWORD FROM TO 'new'") {
    failsToParse
  }

  test("ALTER CURRENT USER SET PASSWORD TO 'new'") {
    failsToParse
  }

  test("ALTER CURRENT USER command finds password literal at correct offset") {
    parsing("ALTER CURRENT USER SET PASSWORD FROM 'current' TO 'new'").shouldVerify { statement =>
      val passwords = statement.findByAllClass[SensitiveStringLiteral].map(l => (new String(l.value, "utf-8"), l.position.offset))
      passwords.toSet should equal(Set("current" -> 37, "new" -> 50))
    }
  }

  test("ALTER CURRENT USER command finds password parameter at correct offset") {
    parsing("ALTER CURRENT USER SET PASSWORD FROM $current TO $new").shouldVerify { statement =>
      val passwords = statement.findByAllClass[SensitiveParameter].map(p => (p.name, p.position.offset))
      passwords.toSet should equal(Set("current" -> 37, "new" -> 49))
    }
  }
}
