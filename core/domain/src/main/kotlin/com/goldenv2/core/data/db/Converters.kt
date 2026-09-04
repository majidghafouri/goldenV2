package com.goldenv2.core.data.db

import androidx.room.TypeConverter
import com.goldenv2.core.domain.model.DomainStrategy
import com.goldenv2.core.domain.model.LogLevel
import com.goldenv2.core.domain.model.NetworkType
import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.ThemeMode
import com.goldenv2.core.domain.model.VpnMode
import com.goldenv2.core.domain.model.VpnStatus
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromProtocol(value: Protocol): String = value.name

    @TypeConverter
    fun toProtocol(value: String): Protocol = Protocol.valueOf(value)

    @TypeConverter
    fun fromNetworkType(value: NetworkType): String = value.name

    @TypeConverter
    fun toNetworkType(value: String): NetworkType = NetworkType.valueOf(value)

    @TypeConverter
    fun fromVpnStatus(value: VpnStatus): String = value.name

    @TypeConverter
    fun toVpnStatus(value: String): VpnStatus = VpnStatus.valueOf(value)

    @TypeConverter
    fun fromVpnMode(value: VpnMode): String = value.name

    @TypeConverter
    fun toVpnMode(value: String): VpnMode = VpnMode.valueOf(value)

    @TypeConverter
    fun fromThemeMode(value: ThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String): ThemeMode = ThemeMode.valueOf(value)

    @TypeConverter
    fun fromLogLevel(value: LogLevel): String = value.name

    @TypeConverter
    fun toLogLevel(value: String): LogLevel = LogLevel.valueOf(value)

    @TypeConverter
    fun fromDomainStrategy(value: DomainStrategy): String = value.name

    @TypeConverter
    fun toDomainStrategy(value: String): DomainStrategy = DomainStrategy.valueOf(value)

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.let { Json.Default.encodeToString(ListSerializer(String.serializer()), it) }

    @TypeConverter
    fun toStringList(value: String?): List<String>? = value?.let { Json.Default.decodeFromString(ListSerializer(String.serializer()), it) }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? = value?.let { Json.Default.encodeToString(MapSerializer(String.serializer(), String.serializer()), it) }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? = value?.let { Json.Default.decodeFromString(MapSerializer(String.serializer(), String.serializer()), it) }

    @TypeConverter
    fun fromStringIntMap(value: Map<String, Int>?): String? = value?.let { Json.Default.encodeToString(MapSerializer(String.serializer(), Int.serializer()), it) }

    @TypeConverter
    fun toStringIntMap(value: String?): Map<String, Int>? = value?.let { Json.Default.decodeFromString(MapSerializer(String.serializer(), Int.serializer()), it) }
}