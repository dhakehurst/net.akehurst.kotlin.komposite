/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.kotlin.komposite.processor

import kotlin.test.Test
import kotlin.test.assertEquals

class test_KompositeDslBuilder {

//    @Test
//    fun f() {
//        val actual = komposite {
//            namespace("test.namespace") {
//                enumType("TestEnum")
//                primitiveType("TestPrimitive")
//                dataType("TestDataTypeAbstract") {}
//                dataType("TestDataType") {
//                    superTypes("TestDataTypeAbstract")
//                    constructorArguments {
//                        reference("refArgProp", "SomeType")
//                        composite("cmpArgProp", "SomeType")
//                        reference("toSelf", "TestDataType")
//                    }
//                    mutableProperties {
//                        reference("refProp", "String")
//                        composite("cmpProp", "TestEnum")
//                        composite("listProp", "List") { typeArgument("String") }
//                        composite("mapProp", "Map") { typeArgument("String"); typeArgument("TestPrimitive") }
//                        composite("setOfMapProp", "Set") {
//                            typeArgument("Map") {
//                                typeArgument("String")
//                                typeArgument("TestPrimitive")
//                            }
//                        }
//                    }
//                }
//                dataType("TestDataTypeWithTypeParameters") {
//                    typeParameters("E")
//                }
//            }
//        }
//
//        assertEquals(listOf("test","namespace"),actual.namespaces[0].path)
////TODO: better test checks
//    }
}