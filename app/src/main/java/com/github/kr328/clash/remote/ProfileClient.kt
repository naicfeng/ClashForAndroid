package com.github.kr328.clash.remote

import android.os.RemoteException
import com.github.kr328.clash.service.IProfileService
import com.github.kr328.clash.service.transact.IStreamCallback
import com.github.kr328.clash.service.transact.ParcelableContainer
import com.github.kr328.clash.service.model.Profile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileClient(private val service: IProfileService) {
    suspend fun acquireUnused(type: Profile.Type, source: String?) = withContext(Dispatchers.IO) {
        service.acquireUnused(type.name, source)
    }

    suspend fun acquireCloned(id: Long) = withContext(Dispatchers.IO) {
        service.acquireCloned(id)
    }

    suspend fun acquireTempUri(id: Long): String? = withContext(Dispatchers.IO) {
        service.acquireTempUri(id)
    }

    suspend fun update(id: Long, metadata: Profile) = withContext(Dispatchers.IO) {
        service.update(id, metadata)
    }

    suspend fun commitAsync(id: Long) = withContext(Dispatchers.IO) {
        CompletableDeferred<Unit>().apply {
            service.commit(id, object : IStreamCallback.Stub() {
                override fun complete() {
                    complete(Unit)
                }

                override fun completeExceptionally(reason: String?) {
                    completeExceptionally(RemoteException(reason))
                }

                override fun send(data: ParcelableContainer?) {}
            })
        }
    }

    suspend fun release(id: Long) = withContext(Dispatchers.IO) {
        service.release(id)
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        service.delete(id)
    }

    suspend fun clear(id: Long) = withContext(Dispatchers.IO) {
        service.clear(id)
    }

    suspend fun queryAll(): Array<Profile> = withContext(Dispatchers.IO) {
        service.queryAll()
    }

    suspend fun queryActive(): Profile? = withContext(Dispatchers.IO) {
        service.queryActive()
    }

    suspend fun queryById(id: Long): Profile? = withContext(Dispatchers.IO) {
        service.queryById(id)
    }

    suspend fun setActive(id: Long) = withContext(Dispatchers.IO) {
        service.setActive(id)
    }
}