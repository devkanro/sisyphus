package com.bybutter.sisyphus.middleware.grpc

import com.bybutter.sisyphus.rpc.AbstractCoroutineStub
import com.bybutter.sisyphus.rpc.CallOptionsInterceptor
import com.bybutter.sisyphus.spi.Ordered
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.AbstractStub
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.core.env.Environment

interface ClientRepository : Ordered {
    fun listClientBeanDefinition(
        beanFactory: ConfigurableListableBeanFactory,
        environment: Environment
    ): List<AbstractBeanDefinition>

    fun getClientFromService(service: Class<*>): Class<*> {
        return service.declaredClasses.firstOrNull { it.simpleName == "Client" }
            ?: throw IllegalStateException("Grpc service must have nested class named 'Client'.")
    }

    fun interceptStub(
        stub: AbstractStub<*>,
        builderInterceptors: Iterable<ClientBuilderInterceptor>,
        interceptors: Iterable<ClientInterceptor>
    ): AbstractStub<*> {
        var result = stub

        for (interceptor in builderInterceptors) {
            result = interceptor.intercept(result)
        }

        return result.withInterceptors(*interceptors.reversed().toTypedArray())
    }

    fun createGrpcChannel(
        target: String,
        builderInterceptors: Iterable<ChannelBuilderInterceptor>,
        lifecycle: ManagedChannelLifecycle
    ): Channel {
        var builder = ManagedChannelBuilder.forTarget(target).usePlaintext().userAgent("Generated by Sisyphus")

        for (interceptor in builderInterceptors) {
            builder = interceptor.intercept(builder)
        }

        return builder.build().apply {
            lifecycle.registerManagedChannel(this)
        }
    }

    fun createGrpcClient(
        target: Class<*>,
        channel: Channel,
        optionsInterceptors: Iterable<CallOptionsInterceptor>,
        callOptions: CallOptions
    ): AbstractStub<*> {
        if (AbstractCoroutineStub::class.java.isAssignableFrom(target)) {
            return target.getDeclaredConstructor(Channel::class.java, Iterable::class.java, CallOptions::class.java)
                .newInstance(channel, optionsInterceptors, callOptions) as AbstractStub<*>
        } else {
            return target.getDeclaredConstructor(Channel::class.java, CallOptions::class.java)
                .newInstance(channel, callOptions) as AbstractStub<*>
        }
    }
}
