package dev.moetz.koarl.backend.persistence

interface DatabaseProvider {

    val crashStorage: CrashStorage

    val appStorage: AppStorage

    val obfuscationMappingStorage: ObfuscationMappingStorage

}