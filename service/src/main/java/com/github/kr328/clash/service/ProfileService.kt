package com.github.kr328.clash.service

import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.RemoteException
import com.github.kr328.clash.service.data.ProfileDao
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.model.asProfile
import com.github.kr328.clash.service.transact.IStreamCallback
import com.github.kr328.clash.service.util.broadcastProfileChanged
import com.github.kr328.clash.service.util.resolveBaseDir
import com.github.kr328.clash.service.util.resolveProfileFile
import com.github.kr328.clash.service.util.resolveTempProfileFile
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProfileService : BaseService() {
    private val service = this
    private val lock = Mutex()
    private val pending = mutableMapOf<Long, Profile>()
    private val tasks = mutableMapOf<Long, IStreamCallback?>()
    private val request = Channel<Unit>(Channel.CONFLATED)

    override fun onBind(intent: Intent?): IBinder? {
        return object : IProfileService.Stub() {
            override fun setActive(id: Long) {
                launch {
                    ProfileDao.setActive(id)

                    service.broadcastProfileChanged()
                }
            }

            override fun commit(id: Long, callback: IStreamCallback?) {
                launch {
                    lock.withLock {
                        tasks[id] = callback

                        request.offer(Unit)
                    }
                }
            }

            override fun release(id: Long) {
                launch {
                    lock.withLock {
                        pending.remove(id)
                    }
                }
            }

            override fun acquireUnused(type: String, source: String?): Long {
                return runBlocking {
                    lock.withLock {
                        val id = generateNextId()

                        pending[id] = Profile(
                            id = id,
                            name = "",
                            type = Profile.Type.valueOf(type),
                            uri = Uri.EMPTY,
                            source = source,
                            active = false,
                            interval = 0,
                            lastModified = 0
                        )

                        service.resolveBaseDir(id).apply {
                            deleteRecursively()
                            mkdirs()
                        }

                        id
                    }
                }
            }

            override fun acquireCloned(id: Long): Long {
                return runBlocking {
                    val clonedId = generateNextId()

                    pending[clonedId] =
                        queryMetadataById(id)?.copy(
                            id = clonedId,
                            active = false,
                            lastModified = 0,
                            type = Profile.Type.FILE
                        ) ?: return@runBlocking -1L

                    clonedId
                }
            }

            override fun queryActive(): Profile? {
                return runBlocking {
                    ProfileDao.queryActive()?.asProfile(service)
                }
            }

            override fun delete(id: Long) {
                launch {
                    lock.withLock {
                        if (pending.remove(id) != null)
                            service.resolveBaseDir(id).deleteRecursively()
                        ProfileDao.remove(id)
                    }

                    ProfileReceiver.cancelNextUpdate(service, id)

                    service.resolveProfileFile(id).delete()
                    service.resolveTempProfileFile(id).delete()
                    service.resolveBaseDir(id).deleteRecursively()

                    service.broadcastProfileChanged()
                }
            }

            override fun clear(id: Long) {
                launch {
                    withContext(Dispatchers.IO) {
                        resolveBaseDir(id).listFiles()?.forEach {
                            it.deleteRecursively()
                        }
                    }

                    service.broadcastProfileChanged()
                }
            }

            override fun queryAll(): Array<Profile> {
                return runBlocking {
                    ProfileDao.queryAll().map { it.asProfile(service) }.toTypedArray()
                }
            }

            override fun queryById(id: Long): Profile? {
                return runBlocking {
                    lock.withLock {
                        queryMetadataById(id)
                    }
                }
            }

            override fun acquireTempUri(id: Long): String? {
                val file = service.resolveProfileFile(id)
                if ( !file.exists() )
                    return null

                val tempFile = service.resolveTempProfileFile(id)

                tempFile.parentFile?.mkdirs()

                file.copyTo(tempFile, overwrite = true)

                return ProfileProvider.resolveUri(service, tempFile).toString()
            }

            override fun update(id: Long, metadata: Profile?) {
                launch {
                    lock.withLock {
                        pending[id] = metadata ?: return@launch
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        launch {
            ProfileReceiver.initialize(service)

            process()
        }
    }

    private suspend fun process() {
        while (isActive) {
            request.receive()

            val ctx = lock.withLock {
                tasks.entries.firstOrNull()?.also {
                    tasks[it.key]
                }
            } ?: continue

            try {
                val metadata = queryMetadataById(ctx.key)
                    ?: throw RemoteException("No such profile")

                ProfileProcessor.createOrUpdate(service, metadata)

                ctx.value?.complete()

                service.broadcastProfileChanged()
            } catch (e: Exception) {
                ctx.value?.completeExceptionally(e.message)
            }
            finally {
                lock.withLock {
                    tasks.remove(ctx.key)
                }
            }

            request.offer(Unit)
        }
    }

    private suspend fun queryMetadataById(id: Long): Profile? {
        return pending[id] ?: ProfileDao.queryById(id)?.asProfile(service)
    }

    private suspend fun generateNextId(): Long {
        return (ProfileDao.queryAllIds() + pending.keys).max()?.plus(1) ?: 0
    }
}