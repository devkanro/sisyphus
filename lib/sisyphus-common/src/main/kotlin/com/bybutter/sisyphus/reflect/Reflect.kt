package com.bybutter.sisyphus.reflect

import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

private fun <T> Type.getTypeArgument(target: Class<T>, index: Int, map: MutableMap<Type, Type>): Type? {
    val raw = when (this) {
        is ParameterizedType -> {
            for ((i, parameter) in (this.rawType as Class<*>).typeParameters.withIndex()) {
                map[parameter] = this.actualTypeArguments[i]
            }
            this.rawType as Class<*>
        }
        is Class<*> -> {
            this
        }
        else -> {
            throw IllegalArgumentException()
        }
    }

    if (raw == target) {
        var result = map[target.typeParameters[index]]
        while (result is TypeVariable<*>) {
            result = map[result]
        }
        return result
    }

    return if (target.isInterface) {
        raw.genericInterfaces.map {
            it.getTypeArgument(target, index, map)
        }.firstOrNull { it != null } ?: raw.genericSuperclass?.getTypeArgument(target, index, map)
    } else {
        raw.genericSuperclass?.getTypeArgument(target, index, map)
    }
}

fun Type.getTypeArgument(target: Class<*>, index: Int): Type {
    return this.getTypeArgument(target, index, mutableMapOf()) ?: throw IllegalArgumentException()
}

val Type.jvm: JvmType
    get() {
        return JvmType.fromType(this)
    }

val Class<*>.jvm: SimpleType
    get() {
        return JvmType.fromType(this) as SimpleType
    }

fun String.toType(): JvmType {
    return JvmType.fromName(this)
}

/**
 * Tool function for suppressing unchecked cast message with 'v as T' in many generic functions.
 */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Any?.uncheckedCast(): T {
    return this as T
}

/**
 * Create or get instance for [KClass], it will find no-arg constructor for create new instance, or return instance for
 * kotlin object, or calling static no-arg function 'provider' to create new instance.
 */
fun <T : Any> KClass<T>.instance(): T {
    this.constructors.singleOrNull { it.parameters.all(KParameter::isOptional) }?.call()?.let { return it }
    this.objectInstance?.let { return it }
    this.java.methods.first {
        it.name == "provider" && Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers) && it.parameterCount == 0
    }.invoke(null)?.let { return it.uncheckedCast() }
    throw IllegalArgumentException("Class should have a single no-arg constructor, or be a 'object', or has static no-arg 'provider' function: $this")
}

fun <T : Any> Class<T>.instance(): T {
    return this.kotlin.instance()
}

object Reflect {
    fun tryGetClass(name: String): Class<*>? {
        return try {
            Class.forName(name)
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    fun classExist(name: String): Boolean {
        return tryGetClass(name) != null
    }

    fun <T> getPrivateField(any: Any, name: String): T? {
        return getPrivateField(any.javaClass, any, name)
    }

    fun <T> getPrivateField(clazz: Class<*>, any: Any, name: String): T? {
        return try {
            clazz.getDeclaredField(name).apply { isAccessible = true }.get(any).uncheckedCast()
        } catch (e: NoSuchFieldException) {
            if (clazz == Any::class.java) {
                throw e
            }
            getPrivateField(clazz.superclass, any, name)
        }
    }

    fun <T> setPrivateField(any: Any, name: String, value: T?) {
        setPrivateField(any.javaClass, any, name, value)
    }

    fun <T> setPrivateField(clazz: Class<*>, any: Any, name: String, value: T?) {
        try {
            clazz.getDeclaredField(name).apply { isAccessible = true }.set(any, value).uncheckedCast<T?>()
        } catch (e: NoSuchFieldException) {
            if (clazz == Any::class.java) {
                throw e
            }
            setPrivateField(clazz.superclass, any, name, value)
        }
    }
}
