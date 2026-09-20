package com.mikonoma.drivinglog.vehicle.data

import kotlinx.coroutines.CoroutineDispatcher

/** The dispatcher for database work. `Dispatchers.IO` is not available from common code. */
expect val ioDispatcher: CoroutineDispatcher
