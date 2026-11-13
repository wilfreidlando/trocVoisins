package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "FirestoreFlows"

/** Écoute temps réel d'une requête : émet la liste complète à chaque changement. */
fun <T : Any> Query.listFlow(mapper: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.w(TAG, "Écoute interrompue : ${error.message}")
            trySend(emptyList())
            return@addSnapshotListener
        }
        if (snapshot != null) {
            trySend(snapshot.documents.mapNotNull { doc -> mapper(doc) })
        }
    }
    awaitClose { registration.remove() }
}

/** Écoute temps réel d'un document. Émet null si le document n'existe pas (ou en cas d'erreur). */
fun <T : Any> DocumentReference.docFlow(mapper: (DocumentSnapshot) -> T?): Flow<T?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.w(TAG, "Écoute interrompue : ${error.message}")
            trySend(null)
            return@addSnapshotListener
        }
        trySend(snapshot?.let { doc -> mapper(doc) })
    }
    awaitClose { registration.remove() }
}

fun serverTime(): FieldValue = FieldValue.serverTimestamp()
