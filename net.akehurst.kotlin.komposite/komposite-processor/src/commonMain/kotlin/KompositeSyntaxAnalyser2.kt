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

package net.akehurst.kotlin.komposite.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.*


data class TypeRefInfo(
    val name:String,
    val args:List<TypeRefInfo>,
    val isNullable:Boolean
) {
    fun toTypeInstance(contextType:TypeDeclaration):TypeInstance {
        val targs = args.map { it.toTypeInstance(contextType) }
        return contextType.namespace.createTypeInstance(contextType, name, targs, isNullable)
    }
}

class KompositeSyntaxAnalyser2 : SyntaxAnalyserByMethodRegistrationAbstract<TypeModel>() {

    class SyntaxAnalyserException : RuntimeException {
        constructor(message: String) : super(message)
    }

    override fun registerHandlers() {
        super.register(this::model)
        super.register(this::namespace)
        super.register(this::qualifiedName)
        super.register(this::import)
        super.register(this::declaration)
        super.register(this::primitive)
        super.register(this::enum)
        super.register(this::collection)
        super.register(this::datatype)
        super.register(this::supertypes)
        super.register(this::property)
        super.register(this::characteristic)
        super.register(this::typeReference)
        super.register(this::typeArgumentList)
    }

    override val embeddedSyntaxAnalyser: Map<String, SyntaxAnalyser<TypeModel>> = emptyMap()

    override fun clear() {
        super.clear()
    }

    // model = namespace* ;
    private fun model(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeModel {
        val result = TypeModelSimple("aTypeModel")
        val namespaces = (children as List<TypeNamespace?>).filterNotNull()
        namespaces.forEach { ns ->
            result.addNamespace(ns)
        }
        return result
    }

    // namespace = 'namespace' qualifiedName '{' import* declaration* '}' ;
    private fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeNamespace {
        val qualifiedName = children[1] as List<String?>
        val imports = (children[3] as List<String?>).filterNotNull()
        val declaration = (children[4] as List<((namespace: TypeNamespace) -> TypeDeclaration)?>).filterNotNull()
        val qn = qualifiedName.joinToString(separator = ".")

        val ns = TypeNamespaceSimple(qn, imports.toMutableList())
        declaration.forEach {
            val dec = it.invoke(ns)
            ns.addDeclaration(dec)
        }

        return ns
    }

    // qualifiedName = [ NAME / '.']+ ;
    private fun qualifiedName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> {
        return children.toSeparatedList<Any?, String, String>().items
    }

    // import = 'import' qualifiedName ;
    private fun import(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        val qualifiedName = children[1] as List<String>
        return qualifiedName.joinToString(separator = ".")
    }

    // declaration = primitive | enum | collection | datatype ;
    private fun declaration(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> TypeDeclaration =
        children[0] as (namespace: TypeNamespace) -> TypeDeclaration

    // primitive = 'primitive' NAME ;
    private fun primitive(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> PrimitiveType {
        val name = children[1] as String
        val result = { namespace: TypeNamespace ->
            PrimitiveTypeSimple(namespace, name)//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        }
        return result
    }

    // enum = 'enum' NAME ;
    private fun enum(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> EnumType {
        val name = children[1] as String
        val result = { namespace: TypeNamespace ->
            //TODO: literals ? maybe
            EnumTypeSimple(namespace, name, emptyList())//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        }
        return result
    }

    // collection = 'collection' NAME '<' typeParameterList '>' ;
    private fun collection(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> CollectionType {
        val name = children[1] as String
        val params = children[3] as List<String>
        val result = { namespace: TypeNamespace ->
            CollectionTypeSimple(namespace, name, params)//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        }
        return result
    }

    // datatype = 'datatype' NAME supertypes? '{' property* '}' ;
    private fun datatype(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> DataType {
        val name = children[1] as String
        val supertypes = children[2] as List<TypeRefInfo>? ?: emptyList()
        val property = (children[4] as List<((DataType) -> PropertyDeclaration)?>).filterNotNull()

        val result = { ns: TypeNamespace ->
            val dt = DataTypeSimple(ns, name)
            supertypes.forEach {
                dt.addSupertype(it.name)
                //(it.type as DataType).addSubtype(dt.name)
            }
            property.forEach {
                val p = it.invoke(dt)
                setResolvers(p.typeInstance as TypeInstanceSimple, dt)
            }
            dt//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        }
        return result
    }

    private fun setResolvers(ti: TypeInstanceSimple, dt: DataType) {
        //ti.namespace = dt.namespace
        ti.typeArguments.forEach { setResolvers(it as TypeInstanceSimple, dt) }
    }

    // supertypes = ':' [ typeReference / ',']+ ;
    private fun supertypes(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TypeRefInfo> {
        return (children[1] as List<Any>).filterNotNull().toSeparatedList<Any, TypeRefInfo, String>().items
    }

    // property = characteristic NAME : typeReference ;
    private fun property(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (StructuredType) -> PropertyDeclaration {
        val characteristics: List<PropertyCharacteristic> = children[0] as List<PropertyCharacteristic>
        val name = children[1] as String
        val typeRef = children[3] as TypeRefInfo
        val result = { owner: StructuredType ->
            val typeInstance = typeRef.toTypeInstance(owner)
            owner.appendPropertyStored(name, typeInstance, characteristics.toSet())
        }
        return result
    }

    // characteristic  = 'val'    // reference, constructor argument
    //                 | 'var'    // reference mutable property
    //                 | 'cal'    // composite, constructor argument
    //                 | 'car'    // composite mutable property
    //                 | 'dis'    // disregard / ignore
    //                 ;
    private fun characteristic(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<PropertyCharacteristic> {
        return when (children[0] as String) {
            "reference-val" -> listOf(PropertyCharacteristic.REFERENCE, PropertyCharacteristic.IDENTITY)
            "reference-var" -> listOf(PropertyCharacteristic.REFERENCE, PropertyCharacteristic.MEMBER)
            "composite-val" -> listOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.IDENTITY)
            "composite-var" -> listOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.MEMBER)
            "dis" -> emptyList()
            else -> error("Value not allowed '${children[0]}'")
        }
    }

    // typeReference = qualifiedName typeArgumentList? '?'?;
    private fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeRefInfo {
        val qualifiedName = children[0] as List<String>
        val typeArgumentList = children[1] as List<TypeRefInfo>? ?: emptyList()
        val qname = qualifiedName.joinToString(separator = ".")
        val isNullable = (children[2] as String?) !=null
        val tr = TypeRefInfo(qname, typeArgumentList, isNullable)//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        return tr
    }

    //typeArgumentList = '<' [ typeReference / ',']+ '>' ;
    private fun typeArgumentList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TypeRefInfo> {
        val list = (children[1] as List<Any>).toSeparatedList<Any, TypeRefInfo, String>().items
        return list
    }

}