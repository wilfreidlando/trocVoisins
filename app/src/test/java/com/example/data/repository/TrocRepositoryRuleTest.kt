package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class TrocRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun publishItem_authenticatedUser_writesToFirestoreAndReadsBack(): Unit = runBlocking {
    val uid = signInTestUser("amadou.kamga@test.cm")
    val docId = "item_test_" + UUID.randomUUID().toString().take(6)
    val payload = mapOf(
      "id" to docId,
      "ownerId" to uid,
      "ownerName" to "Amadou Kamga",
      "title" to "Sac Scolaire Solidaire",
      "description" to "Sac en bon état pour les élèves de Deido",
      "category" to "SCOLAIRE_EDUCATION",
      "condition" to "BON_ETAT",
      "exchangeType" to "DON",
      "city" to "Douala",
      "neighborhood" to "Akwa",
      "status" to "DISPONIBLE",
      "createdAt" to com.google.firebase.Timestamp.now()
    )

    withTimeout(5000L) {
      firestore.collection("items").document(docId).set(payload).await()
    }

    val snapshot = withTimeout(5000L) {
      firestore.collection("items").document(docId).get().await()
    }

    assertTrue(snapshot.exists())
    assertEquals("Sac Scolaire Solidaire", snapshot.getString("title"))
    assertEquals(uid, snapshot.getString("ownerId"))
  }

  @Test
  fun unauthenticatedUser_cannotWriteToItems_throwsPermissionDenied(): Unit = runBlocking {
    auth.signOut()
    val docId = "item_unauth_" + UUID.randomUUID().toString().take(6)
    val payload = mapOf(
      "id" to docId,
      "ownerId" to "fake_uid",
      "ownerName" to "Fake User",
      "title" to "Item Illicite",
      "description" to "Tentative d'écriture anonyme",
      "category" to "AUTRE",
      "condition" to "BON_ETAT",
      "exchangeType" to "DON",
      "city" to "Douala",
      "neighborhood" to "Bastos",
      "status" to "DISPONIBLE",
      "createdAt" to com.google.firebase.Timestamp.now()
    )

    try {
      withTimeout(5000L) {
        firestore.collection("items").document(docId).set(payload).await()
      }
      fail("Expected write to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val fsException = e.unwrapFirestoreException()
      assertNotNull(fsException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
    }
  }

  @Test
  fun disputeCreation_authenticatedUser_writesAndIsIsolatedFromStrangers(): Unit = runBlocking {
    val aliceUid = signInTestUser("alice.deido@test.cm")
    val disputeId = "disp_rule_" + UUID.randomUUID().toString().take(6)
    val disputePayload = mapOf(
      "id" to disputeId,
      "itemId" to "item_123",
      "itemTitle" to "Ventilateur",
      "reportedByUserId" to aliceUid,
      "reporterName" to "Alice Deido",
      "targetUserId" to "bob_uid",
      "reason" to "Objet non fonctionnel à la remise",
      "status" to "OUVERT",
      "createdAt" to com.google.firebase.Timestamp.now()
    )

    withTimeout(5000L) {
      firestore.collection("disputes").document(disputeId).set(disputePayload).await()
    }

    // Alice can read her dispute
    val aliceDoc = withTimeout(5000L) {
      firestore.collection("disputes").document(disputeId).get().await()
    }
    assertTrue(aliceDoc.exists())

    // Charlie cannot read Alice's dispute
    signInTestUser("charlie.stranger@test.cm")
    try {
      withTimeout(5000L) {
        firestore.collection("disputes").document(disputeId).get().await()
      }
      fail("Expected Charlie's read to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val fsException = e.unwrapFirestoreException()
      assertNotNull(fsException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
    }
  }
}
