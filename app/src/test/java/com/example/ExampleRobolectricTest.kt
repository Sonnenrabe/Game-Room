package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Game
import com.example.viewmodel.GameViewModel
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
        assertEquals("PlayTime", appName)
    }

    @Test
    fun `test game playtime formatting`() {
        val game = Game(
            title = "Test Game",
            totalPlaytimeSeconds = 3665L,
            notes = "Completed chapter 1",
            sortOrder = 0
        )
        assertEquals("01:01:05", game.formattedPlaytime())
        assertEquals("01:01:15", game.formattedPlaytime(10L))
        assertEquals("Completed chapter 1", game.notes)
        assertEquals(0, game.sortOrder)
    }

    @Test
    fun `test backup json parsing`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(context)

        val sampleJson = """
            {
              "version": 2,
              "app": "PlayTime",
              "exportedAt": 1730000000000,
              "games": [
                {
                  "title": "Celeste",
                  "totalPlaytimeSeconds": 7200,
                  "isArchived": false,
                  "colorHex": -16717825,
                  "createdAt": 1730000000000,
                  "notes": "Finished Chapter 7 Summit",
                  "sortOrder": 0
                },
                {
                  "title": "Portal",
                  "totalPlaytimeSeconds": 10800,
                  "isArchived": true,
                  "colorHex": -8127393,
                  "createdAt": 1730000000000,
                  "notes": "100% completed",
                  "sortOrder": 1
                }
              ]
            }
        """.trimIndent()

        val result = viewModel.parseBackupSummary(sampleJson)
        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(2, summary.totalCount)
        assertEquals(1, summary.activeCount)
        assertEquals(1, summary.archivedCount)
        assertEquals("Celeste", summary.games[0].title)
        assertEquals("Finished Chapter 7 Summit", summary.games[0].notes)
        assertEquals(0, summary.games[0].sortOrder)
        assertEquals(1, summary.games[1].sortOrder)
    }
}
