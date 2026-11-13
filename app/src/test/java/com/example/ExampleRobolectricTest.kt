package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GrainLevel
import com.example.data.model.GrainRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TrocVoisins", appName)
  }

  @Test
  fun `grain levels follow total earned`() {
    assertEquals(GrainLevel.GRAINE, GrainLevel.forTotal(0))
    assertEquals(GrainLevel.POUSSE, GrainLevel.forTotal(GrainRules.TRUSTED_THRESHOLD))
    assertEquals(GrainLevel.ARBRE, GrainLevel.forTotal(200))
    assertEquals(GrainLevel.FORET, GrainLevel.forTotal(10_000))
  }

  @Test
  fun `a new member can receive a gift before giving`() {
    assertTrue(GrainRules.WELCOME >= GrainRules.DON_RECEIVE_COST)
  }
}
