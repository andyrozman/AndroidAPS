package app.aaps.pump.tandem.common.database.cleanup

import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.pump.tandem.common.database.TandemPumpDatabase
import io.reactivex.rxjava3.core.Completable

object PumpDbCleanup {
    private val ran = java.util.concurrent.atomic.AtomicBoolean(false)

    fun runOnce(db: TandemPumpDatabase,
                aapsSchedulers: AapsSchedulers,
                historyDays: Long = 44, qualifyingEventsDays: Long = 4):
        Completable {
            if (!ran.compareAndSet(false, true)) return Completable.complete()

            val cutoffHistoryMs = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(historyDays)
            val cutoffQEMs = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(qualifyingEventsDays)
            val dao = db.cleanupDao()

            return Completable
                .concatArray(
                    dao.deleteOldHistory(cutoffHistoryMs),
                    dao.deleteOldQualifying(cutoffQEMs),
                    // optional: checkpoint WAL to reclaim space after large deletes
                    Completable.fromAction {
                        //db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                        val sdb = db.openHelper.writableDatabase
                        // FULL or TRUNCATE (TRUNCATE also shrinks the WAL immediately)
                        sdb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { c ->
                            if (c.moveToFirst()) {
                                // val busy = c.getInt(0)      // 0/1
                                // val logPages = c.getInt(1)  // pages in WAL
                                // val ckptPages = c.getInt(2) // pages checkpointed
                                //Log.d("DB", "checkpoint busy=$busy log=$logPages ckpt=$ckptPages")
                            }
                        }
                    }
                )
                .subscribeOn(aapsSchedulers.io)
    }
    
}