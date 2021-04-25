package com.bybutter.sisyphusx.reflect

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses

private fun KType.getTypeArgument(target: KClass<*>, index: Int, map: MutableMap<KTypeParameter, KType>): KType? {
    val raw = when (val kClass = this.classifier) {
        is KClass<*> -> {
            if (!kClass.isSubclassOf(target) && kClass != target) {
                return null
            }

            for ((i, parameter) in kClass.typeParameters.withIndex()) {
                map[parameter] = this.arguments[i].type!!
            }
            kClass
        }
        else -> throw UnsupportedOperationException("Unsupported '$this'.")
    }

    if (raw == target) {
        var result = map[target.typeParameters[index]]!!
        var resultClassifier = result.classifier
        while (resultClassifier is KTypeParameter) {
            result = map[resultClassifier]!!
            resultClassifier = result.classifier
        }
        return result
    }

    return raw.supertypes.map {
        it.getTypeArgument(target, index, map)
    }.firstOrNull { it != null }
}

fun KType.getTypeArgument(target: KClass<*>, index: Int): KType {
    return this.getTypeArgument(target, index, mutableMapOf()) ?: throw IllegalArgumentException()
}

val Type.kotlinType: KType
    get() {
        return when (this) {
            is Class<*> -> {
                this.kotlin.createType()
            }
            is ParameterizedType -> {
                (this.rawType as Class<*>).kotlin.createType(
                    this.actualTypeArguments.map {
                        it.toKTypeProjection()
                    }
                )
            }
            else -> throw UnsupportedOperationException("Unsupported '$this'.")
        }
    }

fun Type.toKTypeProjection(): KTypeProjection {
    return when (this) {
        is Class<*> -> {
            KTypeProjection.invariant(this.kotlinType)
        }
        is ParameterizedType -> {
            KTypeProjection.invariant(this.kotlinType)
        }
        is WildcardType -> {
            when {
                this.lowerBounds.isNotEmpty() -> KTypeProjection.contravariant(this.lowerBounds[0].kotlinType)
                this.upperBounds.isNotEmpty() -> KTypeProjection.covariant(this.upperBounds[0].kotlinType)
                else -> KTypeProjection.STAR
            }
        }
        else -> throw UnsupportedOperationException("Unsupported '$this'.")
    }
}

val KClass<*>.allProperties: List<KProperty1<out Any, *>>
    get() {
        return this.memberProperties + this.superclasses.flatMap { it.allProperties }
    }
