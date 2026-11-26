package com.sportify.app.data.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun <T : Any> Query.asFlow(clazz: Class<T>): Flow<List<T>> = callbackFlow {
    val reg = addSnapshotListener { snap, err ->
        if (err != null) { trySend(emptyList()); return@addSnapshotListener }
        trySend(snap?.toObjects(clazz) ?: emptyList())
    }
    awaitClose { reg.remove() }
}

fun <T : Any> DocumentReference.asFlow(clazz: Class<T>): Flow<T?> = callbackFlow {
    val reg = addSnapshotListener { snap, err ->
        if (err != null) { trySend(null); return@addSnapshotListener }
        trySend(snap?.toObject(clazz))
    }
    awaitClose { reg.remove() }
}
