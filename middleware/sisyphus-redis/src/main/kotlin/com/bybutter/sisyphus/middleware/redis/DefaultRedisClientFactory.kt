package com.bybutter.sisyphus.middleware.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnMissingBean(value = [RedisClientFactory::class])
class DefaultRedisClientFactory : RedisClientFactory {
    private val clients: MutableMap<String, RedisClient> = hashMapOf()

    override fun createClient(property: RedisProperty): RedisClient {
        return createRedisClient(property.host, property.port, property)
    }

    protected fun createRedisClient(host: String, port: Int, property: RedisProperty): RedisClient {
        return clients.getOrPut("$host:$port") {
            RedisClient.create(RedisURI.Builder.redis(host, port).withPassword(property.password).build())
        }
    }
}