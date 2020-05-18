package com.bybutter.sisyphus.protobuf.compiler

import com.bybutter.sisyphus.collection.contentEquals
import com.bybutter.sisyphus.protobuf.primitives.ServiceDescriptorProto
import com.bybutter.sisyphus.rpc.RpcService
import com.bybutter.sisyphus.rpc.ServiceSupport
import com.google.protobuf.DescriptorProtos
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import io.grpc.BindableService
import io.grpc.CallOptions
import io.grpc.ServerServiceDefinition
import io.grpc.ServiceDescriptor
import io.grpc.stub.AbstractStub

class ServiceGenerator(override val parent: FileGenerator, val descriptor: DescriptorProtos.ServiceDescriptorProto) : ProtobufElement() {
    override val kotlinName: String = descriptor.name
    override val protoName: String = descriptor.name

    val supportName by lazy { "${kotlinName}Support" }
    val stubName by lazy { "${kotlinName}Stub" }

    val fullSupportName: String by lazy {
        "${parent.internalKotlinName}.$supportName"
    }
    val fullStubName: String by lazy {
        "${parent.internalKotlinName}.$stubName"
    }

    val serviceType: ClassName by lazy {
        ClassName.bestGuess(fullKotlinName)
    }
    val clientType: ClassName by lazy {
        ClassName.bestGuess("$fullKotlinName.Client")
    }
    val supportType: ClassName by lazy {
        ClassName.bestGuess(fullSupportName)
    }
    val stubType: ClassName by lazy {
        ClassName.bestGuess(fullStubName)
    }

    var path: List<Int> = listOf()
        private set

    override val documentation: String by lazy {
        val location = ensureParent<FileGenerator>().descriptor.sourceCodeInfo.locationList.firstOrNull {
            it.pathList.contentEquals(path)
        } ?: return@lazy ""

        listOf(location.leadingComments, location.trailingComments).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    override fun init() {
        super.init()
        val parent = parent
        path = listOf(DescriptorProtos.FileDescriptorProto.SERVICE_FIELD_NUMBER, parent.descriptor.serviceList.indexOf(descriptor))

        for (method in descriptor.methodList) {
            addElement(ServiceMethodGenerator(this, method))
        }
    }

    fun generate(): TypeSpec {
        return TypeSpec.classBuilder(kotlinName)
            .addKdoc(escapeDoc(documentation))
            .addModifiers(KModifier.ABSTRACT)
            .addSuperinterface(BindableService::class)
            .addType(
                TypeSpec.companionObjectBuilder().superclass(supportType).build()
            )
            .addAnnotation(
                AnnotationSpec.builder(RpcService::class)
                    .addMember("parent = %S", parent.fullProtoName)
                    .addMember("value = %S", protoName)
                    .addMember("client = %T::class", clientType)
                    .build()
            )
            .apply {
                for (child in children) {
                    when (child) {
                        is ServiceMethodGenerator -> {
                            addFunction(child.generateService())
                            addProperty(child.generateServiceHandler())
                        }
                    }
                }
            }
            .addFunction(
                FunSpec.builder("bindService")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(ServerServiceDefinition::class)
                    .addCode(
                        buildCodeBlock {
                            add("return %T.builder(%L)", ServerServiceDefinition::class,
                                buildCodeBlock {
                                    add("%T.newBuilder(%S)", ServiceDescriptor::class, fullProtoName)
                                    for (child in children) {
                                        when (child) {
                                            is ServiceMethodGenerator -> {
                                                add(".addMethod(%L)", child.kotlinName)
                                            }
                                        }
                                    }
                                    add(".setSchemaDescriptor(this)")
                                    add(".build()")
                                }
                            )
                            for (child in children) {
                                when (child) {
                                    is ServiceMethodGenerator -> {
                                        add(".addMethod(%L,·%N)", child.kotlinName, child.generateServiceHandler())
                                    }
                                }
                            }
                            add(".build()")
                        }).build()
            )
            .addType(generateClient())
            // .addTypes(children.mapNotNull { (it as? ServiceMethodGenerator)?.generateHttpHandler() })
            .build()
    }

    private fun generateClient(): TypeSpec {
        return TypeSpec.interfaceBuilder("Client")
            .apply {
                for (child in children) {
                    when (child) {
                        is ServiceMethodGenerator ->
                            addFunction(child.generateClient())
                    }
                }
            }
            .build()
    }

    fun generateStub(): TypeSpec {
        return TypeSpec.classBuilder(stubName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("channel", io.grpc.Channel::class)
                    .addParameter(ParameterSpec.builder("callOptions", CallOptions::class).defaultValue("CallOptions.DEFAULT").build())
                    .build())
            .addAnnotation(INTERNAL_PROTO_API)
            .addSuperinterface(clientType)
            .superclass(AbstractStub::class.asClassName().parameterizedBy(stubType))
            .addSuperclassConstructorParameter("channel")
            .addSuperclassConstructorParameter("callOptions")
            .addFunction(
                FunSpec.builder("build")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("channel", io.grpc.Channel::class)
                    .addParameter("callOptions", CallOptions::class)
                    .returns(stubType)
                    .addStatement("return %T(channel, callOptions)", stubType)
                    .build()
            )
            .apply {
                for (child in children) {
                    when (child) {
                        is ServiceMethodGenerator -> {
                            addFunction(child.generateClientStub())
                        }
                    }
                }
            }
            .build()
    }

    fun generateSupport(): TypeSpec {
        return TypeSpec.classBuilder(supportName)
            .addModifiers(KModifier.ABSTRACT)
            .superclass(ServiceSupport::class)
            .addProperty(
                PropertySpec.builder("descriptor", ServiceDescriptorProto::class)
                    .addModifiers(KModifier.OVERRIDE)
                    .delegate(
                        buildCodeBlock {
                            beginControlFlow("%M", MemberName("kotlin", "lazy"))
                            addStatement("%T.descriptor.service.first{ it.name == %S }", ensureParent<FileGenerator>().fileMetaTypeName, protoName)
                            endControlFlow()
                        }
                    )
                    .build()
            )
            .apply {
                for (child in children) {
                    when (child) {
                        is ServiceMethodGenerator -> {
                            addProperty(child.generateInfo())
                        }
                    }
                }
            }
            .build()
    }
}