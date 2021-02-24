package com.bybutter.sisyphus.dsl.filtering

import com.bybutter.sisyphus.dsl.cel.CelStandardLibrary
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.memberExtensionFunctions
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod

class FilterRuntime(private val std: CelStandardLibrary = CelStandardLibrary()) {

    private val memberFunctions = mutableMapOf<String, MutableList<KFunction<*>>>()

    init {
        for (memberFunction in std.javaClass.kotlin.memberFunctions) {
            if (memberFunction.javaMethod?.canAccess(std) != true) continue
            memberFunctions.getOrPut(memberFunction.name) { mutableListOf() } += memberFunction
        }

        for (memberFunction in std.javaClass.kotlin.memberExtensionFunctions) {
            if (memberFunction.javaMethod?.canAccess(std) != true) continue
            memberFunctions.getOrPut(memberFunction.name) { mutableListOf() } += memberFunction
        }
    }

    fun invoke(function: String, arguments: List<Any?>): Any? {
        val func = memberFunctions[function]?.firstOrNull {
            it.compatibleWith(arguments)
        } ?: throw NoSuchMethodException(
            "Can't find function '$function(${arguments.joinToString(", ") { it?.javaClass?.canonicalName ?: "null" }})' in CEL standard library."
        )
        return func.call(std, *arguments.toTypedArray())
    }

    private fun KFunction<*>.compatibleWith(arguments: List<Any?>): Boolean {
        return compatibleWith(
            listOfNotNull(this.extensionReceiverParameter) + this.valueParameters,
            arguments
        )
    }

    private fun KFunction<*>.compatibleWith(parameters: List<KParameter>, arguments: List<Any?>): Boolean {
        if (parameters.size != arguments.size) return false
        for ((index, parameter) in parameters.withIndex()) {
            val type = arguments[index]?.javaClass
            if (type == null && parameter.type.isMarkedNullable) continue
            if (type == null) return false
            if (!(parameter.type.classifier as KClass<*>).isInstance(arguments[index])) return false
        }

        return true
    }
}
