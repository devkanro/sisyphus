package com.bybutter.sisyphus.middleware.jdbc.support.proto.filter

import com.bybutter.sisyphus.dsl.filtering.FilterStandardLibrary
import org.jooq.Field
import org.jooq.impl.DSL

class FilterJooqLibrary : FilterStandardLibrary() {

    fun currentTimestamp(): Field<*> {
        return DSL.currentTimestamp()
    }

    fun currentTime(): Field<*> {
        return DSL.currentTime()
    }
}
