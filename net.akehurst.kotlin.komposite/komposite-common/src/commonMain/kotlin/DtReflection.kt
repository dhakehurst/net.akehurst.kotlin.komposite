/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.kotlin.komposite.common

import net.akehurst.kotlin.komposite.api.KompositeException
import net.akehurst.kotlinx.reflect.KotlinxReflect
import net.akehurst.kotlinx.reflect.reflect
import net.akehurst.language.typemodel.api.*
import kotlin.reflect.KClass

val TypeDeclaration.clazz: KClass<*> get() = KotlinxReflect.classForName(qualifiedName)

//val PropertyDeclaration.isMutable: Boolean get() = this.datatype.clazz.reflect().isPropertyMutable(this.name)
// reflection isMutable does not work at present!
// assume member properties are mutable unless they are a collection (in which case they are assumed a mutable collection)
val PropertyDeclaration.isMutable: Boolean get() = characteristics.contains(PropertyCharacteristic.MEMBER) && typeInstance.declaration !is CollectionType

fun SingletonType.objectInstance(): Any {
    try {
        val obj = KotlinxReflect.objectInstance<Any>(this.qualifiedName)
        return obj as Any
    } catch (t: Throwable) {
        throw KompositeException("Unable to fetch objectInstance ${this.name} due to ${t.message ?: "Unknown"}")
    }
}

fun DataType.construct(vararg constructorArgs: Any?): Any {
    try {
        val cls = this.clazz
        val obj = cls.reflect().construct(*constructorArgs)
        return obj
    } catch (t: Throwable) {
        throw KompositeException("Unable to construct ${this.name} from ${constructorArgs.toList()} due to ${t.message ?: "Unknown"}")
    }
}

fun PropertyDeclaration.get(obj: Any): Any? {
    val reflect = obj.reflect()
    return reflect.getProperty(this.name)
}
/*
fun DatatypeProperty.set(obj: Any, value: Any?) {
    try {
        val reflect = obj.reflect()

        if (reflect.isPropertyMutable(this.name)) {
            try {
                reflect.setProperty(this.name, value)
                return
            } catch (_: Throwable) {

            }
        }

        val existingValue = reflect.getProperty(this.name)
        if (existingValue is MutableCollection<*> && value is Collection<*>) {
            existingValue.clear()
            (existingValue as MutableCollection<Any>).addAll(value as Collection<Any>)
        } else if (existingValue is MutableMap<*, *> && value is Map<*, *>) {
            existingValue.clear()
            (existingValue as MutableMap<Any, Any>).putAll(value as Map<Any, Any>)
        } else {
            error("Cannot set property ${this.datatype.name}.${this.name} to ${value} because it is not a mutable property or Mutable collection")
        }

    } catch (t: Throwable) {
        throw KompositeException("Unable to set property ${this.datatype.name}.${this.name} to ${value} due to ${t.message ?: "Unknown"}")
    }
}
*/
fun PropertyDeclaration.set(obj: Any, value: Any?) {
    try{
        val reflect = obj.reflect()
        if (this.isMutable) {
            reflect.setProperty(this.name, value)
        } else {
            val existingValue = reflect.getProperty(this.name)
            when {
                (existingValue is MutableCollection<*> && value is Collection<*>) -> {
                    existingValue.clear()
                    (existingValue as MutableCollection<Any>).addAll(value as Collection<Any>)
                }
                (existingValue is MutableCollection<*> && value is Iterable<*>) -> {
                    existingValue.clear()
                    (existingValue as MutableCollection<Any>).addAll(value as Iterable<Any>)
                }
                (existingValue is MutableCollection<*> && value is Array<*>) -> {
                    existingValue.clear()
                    (existingValue as MutableCollection<Any>).addAll(value as Array<Any>)
                }
                (existingValue is MutableMap<*, *> && value is Map<*, *>) -> {
                    existingValue.clear()
                    (existingValue as MutableMap<Any, Any>).putAll(value as Map<Any, Any>)
                }
                else -> error("Cannot set property ${this.owner.name}.${this.name} to ${value} because it is not a mutable property or Mutable collection")
            }
        }

    } catch (t: Throwable) {
        throw KompositeException("Unable to set property ${this.owner.name}.${this.name} to ${value} due to ${t.message ?: "Unknown"}")
    }
}

fun <E : Enum<E>> EnumType.valueOf(name: String): Enum<E>? {
    return this.clazz.reflect().enumValueOf<E>(name)
}