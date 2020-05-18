package com.bybutter.sisyphus.middleware.redis

import org.springframework.boot.context.properties.NestedConfigurationProperty

data class RedisProperty(
    val name: String?,
    val host: String,
    val port: Int,
    val password: String
)

data class RedisProperties(
    @NestedConfigurationProperty
    val redis: Map<String, RedisProperty>
)