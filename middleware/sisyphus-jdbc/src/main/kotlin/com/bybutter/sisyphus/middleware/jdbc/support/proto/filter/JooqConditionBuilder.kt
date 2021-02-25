package com.bybutter.sisyphus.middleware.jdbc.support.proto.filter

import com.bybutter.sisyphus.dsl.filtering.FilterRuntime
import com.bybutter.sisyphus.dsl.filtering.grammar.FilterParser
import com.bybutter.sisyphus.string.unescape
import java.lang.IllegalStateException
import org.jooq.Condition
import org.jooq.Field

interface FilterExpressionValue<T> {
    val value: T
}

data class IntValue(override val value: Long) : FilterExpressionValue<Long>

data class UIntValue(override val value: ULong) : FilterExpressionValue<ULong>

data class FloatValue(override val value: Double) : FilterExpressionValue<Double>

data class StringValue(override val value: String) : FilterExpressionValue<String>

data class BooleanValue(override val value: Boolean) : FilterExpressionValue<Boolean>

data class FieldValue(override val value: Field<*>) : FilterExpressionValue<Field<*>>

abstract class JooqConditionBuilder(val runtime: FilterRuntime = FilterRuntime()) {
    fun visit(filter: FilterParser.FilterContext): Condition? {
        return visit(filter.e ?: return null)
    }

    protected open fun visit(expr: FilterParser.ExpressionContext): Condition? {
        return expr.seq.fold(visit(expr.init)) { cond, seq ->
            cond?.and(visit(seq))
        }
    }

    protected open fun visit(seq: FilterParser.SequenceContext): Condition? {
        return seq.e.fold(visit(seq.init)) { cond, e ->
            cond?.and(visit(e))
        }
    }

    protected open fun visit(factor: FilterParser.FactorContext): Condition? {
        return factor.e.fold(visit(factor.init)) { cond, e ->
            cond?.or(visit(e))
        }
    }

    protected open fun visit(condition: FilterParser.ConditionContext): Condition? {
        return when (condition) {
            is FilterParser.NotConditionContext -> {
                visit(condition.expression())?.not()
            }
            is FilterParser.CompareConditionContext -> {
                val field = visit(condition.left)
                val value = visit(condition.right)
                val op = condition.comparator().text
                buildCondition(field, op, resolveValue(field, value))
            }
            else -> throw IllegalStateException()
        }
    }

    protected open fun visit(member: FilterParser.MemberContext): Field<*>? {
        return resolveMember(member.text)
    }

    protected open fun visit(value: FilterParser.ValueContext): FilterExpressionValue<*>? {
        value.member()?.let { return visit(it)?.let { FieldValue(it) } }
        value.literal()?.let { return visit(it) }
        value.function()?.let { return visit(it) }
        return null
    }

    protected open fun visit(function: FilterParser.FunctionContext): FilterExpressionValue<*>? {
        return runtime.invoke(function.name.text, visit(function.argList()).map { it.value })?.expressionValue()
    }

    protected open fun visit(argList: FilterParser.ArgListContext): List<FilterExpressionValue<*>> {
        return argList.args.mapNotNull { visit(it) }
    }

    protected open fun visit(literal: FilterParser.LiteralContext): FilterExpressionValue<*>? {
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

    protected open fun visit(value: FilterParser.StringContext): String {
        val string = value.text
        return when {
            string.startsWith("\"") -> string.substring(1, string.length - 1)
            string.startsWith("'") -> string.substring(1, string.length - 1)
            else -> throw IllegalStateException("Wrong string token '${value.text}'.")
        }.unescape()
    }

    private fun Any.expressionValue(): FilterExpressionValue<*>? {
        return when (this) {
            is Int, is Long -> IntValue(this.toString().toLong())
            is UInt, is ULong -> UIntValue(this.toString().toULong())
            is Float, is Double -> FloatValue(this.toString().toDouble())
            is Boolean -> BooleanValue(this)
            is String -> StringValue(this)
            else -> throw IllegalStateException("Illegal proto data type '${this?.javaClass}'.")
        }
    }

    protected open fun buildCondition(field: Field<*>?, op: String, value: Any?): Condition? {
        val field = field as? Field<Any> ?: return null
        return when (op) {
            "<=" -> field.le(value)
            "<" -> field.lt(value)
            ">=" -> field.ge(value)
            ">" -> field.gt(value)
            "=" -> field.eq(value ?: return field.isNull)
            "!=" -> field.ne(value ?: return field.isNotNull)
            ":" -> {
                if (value == null) return field.isNull
                val value = value.toString()
                if (value == "*") return field.isNotNull
                if (value.startsWith("*") || value.endsWith("*")) {
                    return field.like(value.replace('*', '%'))
                }
                field.eq(value)
            }
            else -> TODO()
        }
    }

    abstract fun resolveMember(member: String): Field<*>?

    open fun resolveValue(field: Field<*>?, value: FilterExpressionValue<*>?): Any? {
        return value?.value
    }
}
