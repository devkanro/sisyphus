package com.bybutter.sisyphus.dsl.filtering

import com.bybutter.sisyphus.dsl.filtering.grammar.FilterParser
import com.bybutter.sisyphus.protobuf.CustomProtoType
import com.bybutter.sisyphus.protobuf.Message
import com.bybutter.sisyphus.protobuf.ProtoEnum
import com.bybutter.sisyphus.protobuf.primitives.BoolValue
import com.bybutter.sisyphus.protobuf.primitives.BytesValue
import com.bybutter.sisyphus.protobuf.primitives.DoubleValue
import com.bybutter.sisyphus.protobuf.primitives.Duration
import com.bybutter.sisyphus.protobuf.primitives.FloatValue
import com.bybutter.sisyphus.protobuf.primitives.Int32Value
import com.bybutter.sisyphus.protobuf.primitives.Int64Value
import com.bybutter.sisyphus.protobuf.primitives.ListValue
import com.bybutter.sisyphus.protobuf.primitives.NullValue
import com.bybutter.sisyphus.protobuf.primitives.StringValue
import com.bybutter.sisyphus.protobuf.primitives.Struct
import com.bybutter.sisyphus.protobuf.primitives.Timestamp
import com.bybutter.sisyphus.protobuf.primitives.UInt32Value
import com.bybutter.sisyphus.protobuf.primitives.UInt64Value
import com.bybutter.sisyphus.protobuf.primitives.Value
import com.bybutter.sisyphus.protobuf.primitives.string
import com.bybutter.sisyphus.security.base64
import com.bybutter.sisyphus.string.unescape

interface FilterExpressionValue<T> {
    val value: T
}

data class IntValue(override val value: Long) : FilterExpressionValue<Long>

data class UIntValue(override val value: ULong) : FilterExpressionValue<ULong>

data class FloatValue(override val value: Double) : FilterExpressionValue<Double>

data class StringValue(override val value: String) : FilterExpressionValue<String>

data class BooleanValue(override val value: Boolean) : FilterExpressionValue<Boolean>

data class MessageValue(override val value: Message<*, *>) : FilterExpressionValue<Message<*, *>>

data class ByteArrayValue(override val value: ByteArray) : FilterExpressionValue<ByteArray>

data class ListValue(override val value: List<*>) : FilterExpressionValue<List<*>>

data class TimestampValue(override val value: Timestamp) : FilterExpressionValue<Timestamp>

data class DurationValue(override val value: Duration) : FilterExpressionValue<Duration>

class MessageFilter(filter: String, val runtime: FilterRuntime = FilterRuntime()) : (Message<*, *>) -> Boolean {
    private val filter: FilterParser.FilterContext = FilterDsl.parse(filter)

    fun filter(message: Message<*, *>): Boolean {
        return invoke(message)
    }

    override fun invoke(message: Message<*, *>): Boolean {
        val expr = filter.e ?: return true
        return visit(message, expr)
    }

    private fun visit(message: Message<*, *>, expr: FilterParser.ExpressionContext): Boolean {
        return expr.seq.fold(visit(message, expr.init)) { cond, seq ->
            cond.and(visit(message, seq))
        }
    }

    private fun visit(message: Message<*, *>, seq: FilterParser.SequenceContext): Boolean {
        return seq.e.fold(visit(message, seq.init)) { cond, e ->
            cond.and(visit(message, e))
        }
    }

    private fun visit(message: Message<*, *>, factor: FilterParser.FactorContext): Boolean {
        return factor.e.fold(visit(message, factor.init)) { cond, e ->
            cond.or(visit(message, e))
        }
    }

    private fun visit(message: Message<*, *>, condition: FilterParser.ConditionContext): Boolean {
        return when (condition) {
            is FilterParser.NotConditionContext -> {
                !visit(message, condition)
            }
            is FilterParser.CompareConditionContext -> {
                val left = visit(message, condition.left) ?: return false
                val right = visit(message, condition.right) ?: return false
                val op = condition.comparator().text ?: return false
                calculation(left, op, right)
            }
            else -> false
        }
    }

    private fun calculation(left: Any, op: String, right: Any): Boolean {
        return when (op) {
            "<=", "<", ">", ">=" -> {
                val result = DynamicOperator.compare(left.toString(), right.toString())
                when (op) {
                    "<=" -> result <= 0
                    "<" -> result < 0
                    ">=" -> result >= 0
                    ">" -> result > 0
                    else -> false
                }
            }
            "=" -> {
                DynamicOperator.equals(left.toString(), right.toString())
            }
            "!=" -> {
                !DynamicOperator.equals(left.toString(), right.toString())
            }
            ":" -> {
                if (right == "*") return true

                when (left) {
                    is Message<*, *> -> {
                        left.has(right.toString())
                    }
                    is List<*> -> left.any {
                        DynamicOperator.equals(it?.toString(), right.toString())
                    }
                    is Map<*, *> -> {
                        left.containsKey(right)
                    }
                    else -> DynamicOperator.equals(left.toString(), right.toString())
                }
            }
            else -> TODO()
        }
    }

    private fun visit(message: Message<*, *>, member: FilterParser.MemberContext): FilterExpressionValue<*>? {
        val field = member.names.firstOrNull()?.text ?: return null
        var memberValue = message.get<Any?>(field)
        for (i in 1 until member.names.size) {
            val childFieldName = member.names[i].text
            memberValue = when (memberValue) {
                is Message<*, *> -> {
                    memberValue.get<Any?>(childFieldName).protoNormalizing() ?: return null
                }
                is List<*> -> {
                    val int = childFieldName.toIntOrNull() ?: return null
                    if (int >= memberValue.size) return null
                    memberValue[int]
                }
                is Map<*, *> -> {
                    if (!memberValue.containsKey(childFieldName)) return null
                    memberValue[childFieldName]
                }
                else -> return null
            }
        }
        return memberValue?.expressionValue()
    }

    private fun visit(message: Message<*, *>, value: FilterParser.ValueContext): FilterExpressionValue<*>? {
        value.member()?.let { return visit(message, it) }
        value.literal()?.let { return visit(message, it) }
        value.function()?.let { return visit(message, it) }
        return null
    }

    private fun visit(message: Message<*, *>, literal: FilterParser.LiteralContext): FilterExpressionValue<*>? {
        return when (literal) {
            is FilterParser.IntContext -> IntValue(literal.text.toLong())
            is FilterParser.UintContext -> UIntValue(literal.text.substring(0, literal.text.length - 1).toULong())
            is FilterParser.DoubleContext -> FloatValue(literal.text.toDouble())
            is FilterParser.StringContext -> StringValue(visit(literal))
            is FilterParser.BoolTrueContext -> BooleanValue(true)
            is FilterParser.BoolFalseContext -> BooleanValue(false)
            is FilterParser.NullContext -> null
            else -> throw UnsupportedOperationException("Unsupported literal expression '${literal.text}'.")
        }
    }

    private fun visit(message: Message<*, *>, function: FilterParser.FunctionContext): FilterExpressionValue<*>? {
        return runtime.invoke(function.name.text, visit(message, function.argList()).map { it.value })?.expressionValue()
    }

    private fun visit(message: Message<*, *>, argList: FilterParser.ArgListContext): List<FilterExpressionValue<*>> {
        return argList.args.mapNotNull { visit(message, it) }
    }

    private fun visit(value: FilterParser.StringContext): String {
        val string = value.text
        return when {
            string.startsWith("\"") -> string.substring(1, string.length - 1)
            string.startsWith("'") -> string.substring(1, string.length - 1)
            else -> throw java.lang.IllegalStateException("Wrong string token '${value.text}'.")
        }.unescape()
    }

    private fun Any.expressionValue(): FilterExpressionValue<*>? {
        return when (this) {
            is Int64Value -> IntValue(this.value)
            is Int32Value -> IntValue(this.value.toLong())
            is Int -> IntValue(this.toLong())
            is Long -> IntValue(this)
            is UInt64Value -> UIntValue(this.value)
            is UInt32Value -> UIntValue(this.value.toULong())
            is UInt -> UIntValue(this.toULong())
            is ULong -> UIntValue(this)
            is FloatValue -> FloatValue(this.value.toDouble())
            is Float -> FloatValue(this.toDouble())
            is Double -> FloatValue(this)
            is DoubleValue -> FloatValue(this.value)
            is BoolValue -> BooleanValue(this.value)
            is Boolean -> BooleanValue(this)
            is StringValue -> StringValue(this.value)
            is String -> StringValue(this)
            is Message<*, *> -> MessageValue(this)
            is ByteArray -> ByteArrayValue(this)
            is List<*> -> ListValue(this)
            is Timestamp -> TimestampValue(this)
            is Duration -> DurationValue(this)
            else -> throw java.lang.IllegalStateException("Illegal proto data type '${this?.javaClass}'.")
        }
    }

    private fun Any?.protoNormalizing(): Any? {
        return when (this) {
            is ByteArray -> this.base64()
            is ListValue -> this.values.map { it.protoNormalizing() }
            is DoubleValue -> this.value.toString()
            is FloatValue -> this.value.toString()
            is Int64Value -> this.value.toString()
            is UInt64Value -> this.value.toString()
            is Int32Value -> this.value.toString()
            is UInt32Value -> this.value.toString()
            is BoolValue -> this.value.toString()
            is StringValue -> this.value
            is BytesValue -> this.value.base64()
            is Duration -> string()
            is Timestamp -> string()
            is NullValue -> null
            is Struct -> this.fields.mapValues { it.value.protoNormalizing() }
            is Value -> when (val kind = this.kind) {
                is Value.Kind.BoolValue -> kind.value.toString()
                is Value.Kind.ListValue -> kind.value.protoNormalizing()
                is Value.Kind.NullValue -> null
                is Value.Kind.NumberValue -> kind.value.toString()
                is Value.Kind.StringValue -> kind.value
                is Value.Kind.StructValue -> kind.value.protoNormalizing()
                null -> null
                else -> throw IllegalStateException("Illegal proto value type '${kind.javaClass}'.")
            }
            is ProtoEnum -> this.proto
            is List<*> -> this.map { it.protoNormalizing() }
            is Map<*, *> -> this.mapValues { it.value.protoNormalizing() }
            is CustomProtoType<*> -> this.value().protoNormalizing()
            null -> null
            is Int, is UInt, is Long, is ULong, is Float, is Double, is Boolean -> this.toString()
            is String, is Message<*, *> -> this
            else -> throw IllegalStateException("Illegal proto data type '${this.javaClass}'.")
        }
    }
}
