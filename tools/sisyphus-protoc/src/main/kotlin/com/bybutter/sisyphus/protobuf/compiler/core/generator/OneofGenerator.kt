package com.bybutter.sisyphus.protobuf.compiler.core.generator

import com.bybutter.sisyphus.protobuf.compiler.GroupedGenerator
import com.bybutter.sisyphus.protobuf.compiler.RuntimeTypes
import com.bybutter.sisyphus.protobuf.compiler.SortableGenerator
import com.bybutter.sisyphus.protobuf.compiler.clearFunction
import com.bybutter.sisyphus.protobuf.compiler.constructor
import com.bybutter.sisyphus.protobuf.compiler.core.state.FieldImplementationGeneratingState
import com.bybutter.sisyphus.protobuf.compiler.core.state.MessageImplementationGeneratingState
import com.bybutter.sisyphus.protobuf.compiler.core.state.MessageInterfaceGeneratingState
import com.bybutter.sisyphus.protobuf.compiler.core.state.MutableMessageInterfaceGeneratingState
import com.bybutter.sisyphus.protobuf.compiler.core.state.OneofValueTypeGeneratingState
import com.bybutter.sisyphus.protobuf.compiler.core.state.advance
import com.bybutter.sisyphus.protobuf.compiler.defaultValue
import com.bybutter.sisyphus.protobuf.compiler.elementType
import com.bybutter.sisyphus.protobuf.compiler.fieldType
import com.bybutter.sisyphus.protobuf.compiler.function
import com.bybutter.sisyphus.protobuf.compiler.getter
import com.bybutter.sisyphus.protobuf.compiler.hasFunction
import com.bybutter.sisyphus.protobuf.compiler.implements
import com.bybutter.sisyphus.protobuf.compiler.kInterface
import com.bybutter.sisyphus.protobuf.compiler.mutableFieldType
import com.bybutter.sisyphus.protobuf.compiler.name
import com.bybutter.sisyphus.protobuf.compiler.plusAssign
import com.bybutter.sisyphus.protobuf.compiler.property
import com.bybutter.sisyphus.protobuf.compiler.setter
import com.bybutter.sisyphus.protobuf.compiler.type
import com.bybutter.sisyphus.string.toPascalCase
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.buildCodeBlock

class OneOfInterfaceGenerator :
    com.bybutter.sisyphus.protobuf.compiler.GroupedGenerator<MessageInterfaceGeneratingState> {
    override fun generate(state: MessageInterfaceGeneratingState): Boolean {
        for (oneof in state.descriptor.oneofs) {
            state.target.property(
                oneof.fieldName(),
                oneof.oneOfClassName().parameterizedBy(TypeVariableName("*")).copy(true)
            )

            state.target.addType(kInterface(oneof.oneOfName()) {
                OneofValueTypeGeneratingState(state, oneof, this).advance()
            })
        }
        return true
    }
}

class OneofValueBasicGenerator :
    com.bybutter.sisyphus.protobuf.compiler.GroupedGenerator<OneofValueTypeGeneratingState> {
    override fun generate(state: OneofValueTypeGeneratingState): Boolean {
        state.target.apply {
            this implements RuntimeTypes.ONE_OF_VALUE.parameterizedBy(TypeVariableName("T"))
            this.addTypeVariable(TypeVariableName("T"))

            for (field in state.descriptor.parent.fields) {
                if (field.oneof() == state.descriptor) {
                    type(field.descriptor.name.toPascalCase()) {
                        this += KModifier.DATA
                        this implements state.descriptor.oneOfClassName().parameterizedBy(field.elementType())
                        constructor {
                            addParameter("value", field.elementType())
                        }
                        property("value", field.elementType()) {
                            this += KModifier.OVERRIDE
                            initializer("value")
                        }
                    }
                }
            }
        }
        return true
    }
}

class OneOfMutableInterfaceGenerator :
    com.bybutter.sisyphus.protobuf.compiler.GroupedGenerator<MutableMessageInterfaceGeneratingState> {
    override fun generate(state: MutableMessageInterfaceGeneratingState): Boolean {
        for (oneof in state.descriptor.oneofs) {
            state.target.property(
                oneof.fieldName(),
                oneof.oneOfClassName().parameterizedBy(TypeVariableName("*")).copy(true)
            ) {
                this += KModifier.OVERRIDE
                mutable()
            }
        }
        return true
    }
}

class OneOfImplementationGenerator :
    com.bybutter.sisyphus.protobuf.compiler.GroupedGenerator<MessageImplementationGeneratingState> {
    override fun generate(state: MessageImplementationGeneratingState): Boolean {
        for (oneof in state.descriptor.oneofs) {
            state.target.property(
                oneof.fieldName(),
                oneof.oneOfClassName().parameterizedBy(TypeVariableName("*")).copy(true)
            ) {
                this += KModifier.OVERRIDE
                mutable()
                initializer("null")
            }
        }
        return true
    }
}

class OneofFieldImplementationInterceptorGenerator : com.bybutter.sisyphus.protobuf.compiler.GroupedGenerator<FieldImplementationGeneratingState>,
    com.bybutter.sisyphus.protobuf.compiler.SortableGenerator<FieldImplementationGeneratingState> {
    override val group: String get() = MessageImplementationFieldBasicGenerator::class.java.canonicalName
    override val order: Int = -1000

    override fun generate(state: FieldImplementationGeneratingState): Boolean {
        val oneOf = state.descriptor.oneof() ?: return false

        state.target.property(state.descriptor.name(), state.descriptor.mutableFieldType()) {
            this += KModifier.OVERRIDE
            mutable()
            getter {
                if (state.descriptor.mutableFieldType().isNullable) {
                    addStatement(
                        "return (%N as? %T)?.value",
                        oneOf.fieldName(),
                        oneOf.oneOfClassName().nestedClass(state.descriptor.descriptor.name.toPascalCase())
                    )
                } else {
                    addStatement(
                        "return (%N as? %T)?.value ?: %L",
                        oneOf.fieldName(),
                        oneOf.oneOfClassName().nestedClass(state.descriptor.descriptor.name.toPascalCase()),
                        state.descriptor.defaultValue()
                    )
                }
            }
            setter {
                addParameter("value", state.descriptor.mutableFieldType())
                addCode(buildCodeBlock {
                    if (state.descriptor.mutableFieldType().isNullable) {
                        addStatement(
                            "%N = value?.let { %T(it) }",
                            oneOf.fieldName(),
                            oneOf.oneOfClassName().nestedClass(state.descriptor.descriptor.name.toPascalCase())
                        )
                    } else {
                        addStatement(
                            "%N = %T(value)",
                            oneOf.fieldName(),
                            oneOf.oneOfClassName().nestedClass(state.descriptor.descriptor.name.toPascalCase())
                        )
                    }
                })
            }
        }

        state.target.function(state.descriptor.hasFunction()) {
            this += KModifier.OVERRIDE
            returns(Boolean::class.java)
            addStatement(
                "return %N is %T",
                oneOf.fieldName(),
                oneOf.fieldName(),
                oneOf.oneOfClassName().nestedClass(state.descriptor.descriptor.name.toPascalCase())
            )
        }

        state.target.function(state.descriptor.clearFunction()) {
            this += KModifier.OVERRIDE
            returns(state.descriptor.fieldType().copy(true))
            beginControlFlow(
                "return (%N as? %T)?.value?.also",
                oneOf.fieldName(),
                oneOf.fieldName(),
                oneOf.oneOfClassName().nestedClass(state.descriptor.descriptor.name.toPascalCase())
            )
            addStatement("%N = null", oneOf.fieldName())
            endControlFlow()
        }

        return true
    }
}