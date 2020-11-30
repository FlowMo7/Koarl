package dev.moetz.koarl.android.data.local

import android.content.Context
import androidx.room.*
import dev.moetz.koarl.android.data.local.model.*
import dev.moetz.koarl.api.model.CrashUploadRequestBody
import dev.moetz.koarl.api.serializer.DateTimeSerializer


@Dao
internal abstract class CrashDao {

    //
    // SELECT CRASHES
    //

    @Query("SELECT * FROM crash")
    protected abstract suspend fun selectAllLocalCrashEntries(): List<LocalCrash>

    @Query("SELECT * FROM throwable")
    protected abstract suspend fun selectAllLocalThrowableEntries(): List<LocalThrowable>

    @Query("SELECT * FROM stackTrace ORDER BY throwableId, stackTracePosition ASC")
    protected abstract suspend fun selectAllLocalStackTraceElementEntries(): List<LocalStackTraceElement>

    @Transaction
    open suspend fun getCrashes(): List<CrashUploadRequestBody.ApiCrash> {
        val localCrashes = selectAllLocalCrashEntries()

        return if (localCrashes.isNotEmpty()) {
            // make the extra queries only if there are crash entries found in the first place to save time.
            val throwables = selectAllLocalThrowableEntries()
            val stackTraceElements = selectAllLocalStackTraceElementEntries()

            localCrashes.map { localCrash ->
                var latestThrowableCause: CrashUploadRequestBody.ApiCrash.ApiThrowable? = null

                val apiThrowable = throwables
                    .asSequence()
                    .filter { it.crashUUID == localCrash.uuid }
                    .sortedByDescending { it.causeDepth }
                    .map { throwable ->
                        val stackTrace = stackTraceElements
                            .asSequence()
                            .filter { it.throwableId == throwable.primaryKey }
                            .sortedBy { it.stackTracePosition }
                            .map { it.asApiStackTraceElement }
                            .toList()

                        throwable
                            .toApiThrowable(
                                stackTrace,
                                latestThrowableCause
                            )
                            .also { latestThrowableCause = it }
                    }
                    .last()



                CrashUploadRequestBody.ApiCrash(
                    uuid = localCrash.uuid,
                    isFatal = localCrash.isFatal,
                    inForeground = localCrash.inForeground,
                    dateTime = DateTimeSerializer.deserializeFromString(localCrash.dateTime),
                    throwable = apiThrowable,
                    deviceState = localCrash.deviceState.asApiDeviceState
                )
            }
        } else {
            emptyList()
        }
    }


    //
    // INSERT CRASH
    //

    @Insert
    protected abstract suspend fun insertLocalCrashEntry(localCrash: LocalCrash)

    @Insert
    protected abstract suspend fun insertLocalThrowableEntryAndReturnPrimaryKey(localThrowable: LocalThrowable): Long

    @Insert
    protected abstract suspend fun insertLocalStackTraceElementEntry(stackTraceElement: LocalStackTraceElement)

    @Transaction
    open suspend fun insert(crash: CrashUploadRequestBody.ApiCrash) {
        insertLocalCrashEntry(LocalCrash(crash))

        var throwable: CrashUploadRequestBody.ApiCrash.ApiThrowable? = crash.throwable
        var causeDepth = 0
        while (throwable != null) {
            val throwableId =
                insertLocalThrowableEntryAndReturnPrimaryKey(
                    LocalThrowable(
                        crash.uuid,
                        causeDepth,
                        throwable
                    )
                )

            throwable.stackTrace.forEachIndexed { index, apiStackTraceElement ->
                insertLocalStackTraceElementEntry(
                    LocalStackTraceElement(
                        throwableId = throwableId,
                        stackTracePosition = index,
                        apiStackTraceElement = apiStackTraceElement
                    )
                )
            }


            throwable = throwable.cause
            causeDepth++
        }
    }


    //
    // DELETE CRASH
    //

    @Query("DELETE FROM throwable WHERE crash IN (:crashUUIDs)")
    protected abstract suspend fun deleteLocalThrowableEntriesByCrashUUIDs(crashUUIDs: List<String>)

    @Query("SELECT primaryKey FROM throwable WHERE crash IN (:crashUUIDs)")
    protected abstract suspend fun selectLocalThrowableEntryIdsByCrashUUIDs(crashUUIDs: List<String>): List<Long>

    @Query("DELETE FROM stackTrace WHERE throwableId IN (:throwableIds)")
    protected abstract suspend fun deleteLocalStackTraceElementEntriesByThrowableIds(throwableIds: List<Long>)

    @Query("DELETE FROM crash WHERE uuid IN (:crashUUIDs)")
    protected abstract suspend fun deleteLocalCrashEntriesByUUIDs(crashUUIDs: List<String>)

    @Transaction
    open suspend fun deleteCrashes(crashUUIDs: List<String>) {
        // Safe-construct here:
        // Having too many SQL variables in an IN clause will cause trouble.
        // Therefore, batch it here.
        crashUUIDs.chunked(25).forEach { chunkedUUIDs ->
            val throwableIds = selectLocalThrowableEntryIdsByCrashUUIDs(chunkedUUIDs)
            deleteLocalStackTraceElementEntriesByThrowableIds(throwableIds)
            deleteLocalThrowableEntriesByCrashUUIDs(chunkedUUIDs)
            deleteLocalCrashEntriesByUUIDs(chunkedUUIDs)
        }
    }

}

@Database(
    entities = [
        LocalCrash::class, LocalThrowable::class, LocalStackTraceElement::class,
        LocalDeviceState::class, LocalAppData::class
    ],
    exportSchema = false,
    version = 2
)
internal abstract class LocalRoomDatabase : RoomDatabase() {

    abstract fun crashDao(): CrashDao

}


class RoomLocalRepo(context: Context) : LocalRepo {

    private val roomDatabase: LocalRoomDatabase by lazy {
        Room
            .databaseBuilder(
                context.applicationContext,
                LocalRoomDatabase::class.java,
                "crash_db"
            )
            .fallbackToDestructiveMigration()   //TODO
            .build()
    }


    override suspend fun addCrash(crash: CrashUploadRequestBody.ApiCrash) {
        roomDatabase.crashDao().insert(crash)
    }

    override suspend fun getCrashes(): List<CrashUploadRequestBody.ApiCrash> {
        return roomDatabase.crashDao().getCrashes()
    }

    override suspend fun removeCrashes(ids: List<String>) {
        roomDatabase.crashDao().deleteCrashes(ids)
    }

}