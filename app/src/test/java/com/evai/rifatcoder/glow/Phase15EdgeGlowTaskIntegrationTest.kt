package com.evai.rifatcoder.glow

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.evai.rifatcoder.data.glow.EdgeGlowStateHolder
import com.evai.rifatcoder.data.voice.CentralVoiceState
import com.evai.rifatcoder.domain.automation.AutomationAction
import com.evai.rifatcoder.domain.automation.AutomationPlan
import com.evai.rifatcoder.domain.automation.AutomationStep
import com.evai.rifatcoder.domain.automation.AutomationTaskPlanner
import com.evai.rifatcoder.domain.automation.AutomationTaskStatus
import com.evai.rifatcoder.domain.glow.EdgeGlowColorOption
import com.evai.rifatcoder.domain.glow.EdgeGlowPreferences
import com.evai.rifatcoder.domain.glow.EdgeGlowState
import com.evai.rifatcoder.domain.tool.AiToolRegistry
import com.evai.rifatcoder.ui.automation.AutomationCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase15EdgeGlowTaskIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        EdgeGlowStateHolder.resetToIdle()
    }

    @After
    fun tearDown() {
        EdgeGlowStateHolder.resetToIdle()
        Dispatchers.resetMain()
    }

    // 1. Voice-to-glow state transitions: IDLE -> LISTENING -> THINKING -> SPEAKING -> IDLE
    @Test
    fun testVoiceToGlowTransitions() {
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)

        EdgeGlowStateHolder.updateVoiceState(CentralVoiceState.LISTENING, currentSoundLevel = 0.6f)
        assertEquals(EdgeGlowState.LISTENING, EdgeGlowStateHolder.glowState.value)
        assertEquals(0.6f, EdgeGlowStateHolder.soundLevel.value, 0.01f)

        EdgeGlowStateHolder.updateVoiceState(CentralVoiceState.PROCESSING)
        assertEquals(EdgeGlowState.THINKING, EdgeGlowStateHolder.glowState.value)

        EdgeGlowStateHolder.updateVoiceState(CentralVoiceState.SPEAKING, currentSoundLevel = 0.8f)
        assertEquals(EdgeGlowState.SPEAKING, EdgeGlowStateHolder.glowState.value)
        assertEquals(0.8f, EdgeGlowStateHolder.soundLevel.value, 0.01f)

        EdgeGlowStateHolder.updateVoiceState(CentralVoiceState.IDLE)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
    }

    // 2. Task-to-glow state transitions: Planning -> WaitingConfirmation -> Executing -> Success
    @Test
    fun testTaskToGlowTransitions() = runBlocking {
        // Step A: Planning
        EdgeGlowStateHolder.onTaskPlanning("Turn on flashlight")
        assertEquals(EdgeGlowState.THINKING, EdgeGlowStateHolder.glowState.value)
        assertEquals(AutomationTaskStatus.Planning, EdgeGlowStateHolder.currentTaskStatus.value)

        // Step B: Waiting for confirmation
        EdgeGlowStateHolder.onTaskWaitingForConfirmation("Format data", "Consequential data alteration")
        assertEquals(EdgeGlowState.WAITING_FOR_CONFIRMATION, EdgeGlowStateHolder.glowState.value)
        assertEquals(AutomationTaskStatus.WaitingForConfirmation, EdgeGlowStateHolder.currentTaskStatus.value)

        // Step C: Executing
        EdgeGlowStateHolder.onTaskExecuting(stepIndex = 1, totalSteps = 2, description = "Toggling torch hardware")
        assertEquals(EdgeGlowState.EXECUTING, EdgeGlowStateHolder.glowState.value)
        assertEquals(0.5f, EdgeGlowStateHolder.taskStepProgress.value, 0.01f)

        // Step D: Step 2 Executing
        EdgeGlowStateHolder.onTaskExecuting(stepIndex = 2, totalSteps = 2, description = "Verifying torch state")
        assertEquals(EdgeGlowState.EXECUTING, EdgeGlowStateHolder.glowState.value)
        assertEquals(1.0f, EdgeGlowStateHolder.taskStepProgress.value, 0.01f)

        // Step E: Verified Success
        EdgeGlowStateHolder.onTaskSuccess("Torch successfully toggled.", returnDelayMs = 50L)
        assertEquals(EdgeGlowState.SUCCESS, EdgeGlowStateHolder.glowState.value)

        // Wait for transient feedback return
        var elapsed = 0L
        while (EdgeGlowStateHolder.glowState.value != EdgeGlowState.IDLE && elapsed < 1000L) {
            Thread.sleep(25)
            elapsed += 25L
        }
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
    }

    // 3. Task failure and cancellation transitions
    @Test
    fun testTaskFailureAndCancellation() = runBlocking {
        // Failure flow
        EdgeGlowStateHolder.onTaskExecuting(1, 1, "Testing faulty action")
        assertEquals(EdgeGlowState.EXECUTING, EdgeGlowStateHolder.glowState.value)

        EdgeGlowStateHolder.onTaskFailed("Hardware sensor missing", returnDelayMs = 50L)
        assertEquals(EdgeGlowState.FAILED, EdgeGlowStateHolder.glowState.value)

        var elapsed = 0L
        while (EdgeGlowStateHolder.glowState.value != EdgeGlowState.IDLE && elapsed < 1000L) {
            Thread.sleep(25)
            elapsed += 25L
        }
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)

        // Cancellation flow
        EdgeGlowStateHolder.onTaskWaitingForConfirmation("Delete all notes", "Irreversible action")
        assertEquals(EdgeGlowState.WAITING_FOR_CONFIRMATION, EdgeGlowStateHolder.glowState.value)

        EdgeGlowStateHolder.onTaskCancelled(returnDelayMs = 50L)
        assertEquals(EdgeGlowState.CANCELLED, EdgeGlowStateHolder.glowState.value)

        elapsed = 0L
        while (EdgeGlowStateHolder.glowState.value != EdgeGlowState.IDLE && elapsed < 1000L) {
            Thread.sleep(25)
            elapsed += 25L
        }
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
    }

    // 4. Multi-step task progress feedback: Glow remains EXECUTING between steps
    @Test
    fun testMultiStepProgressKeepsGlowActive() {
        val totalSteps = 4
        for (step in 1..totalSteps) {
            EdgeGlowStateHolder.onTaskExecuting(step, totalSteps, "Executing step $step")
            assertEquals(EdgeGlowState.EXECUTING, EdgeGlowStateHolder.glowState.value)

            EdgeGlowStateHolder.onTaskStepCompleted(step, totalSteps)
            // Glow MUST remain EXECUTING between steps
            assertEquals(EdgeGlowState.EXECUTING, EdgeGlowStateHolder.glowState.value)
            assertEquals(step.toFloat() / totalSteps.toFloat(), EdgeGlowStateHolder.taskStepProgress.value, 0.01f)
        }
    }

    // 5. State arbitration: Voice state does NOT overwrite active task execution
    @Test
    fun testTaskExecutionPrecedenceOverVoice() {
        // When task is running
        EdgeGlowStateHolder.onTaskExecuting(1, 2, "Running critical task")
        assertEquals(EdgeGlowState.EXECUTING, EdgeGlowStateHolder.glowState.value)

        // Simultaneous voice state event arrives (e.g. background listener)
        EdgeGlowStateHolder.updateVoiceState(CentralVoiceState.IDLE)
        // Must remain EXECUTING because task execution takes strict priority
        assertEquals(EdgeGlowState.EXECUTING, EdgeGlowStateHolder.glowState.value)

        EdgeGlowStateHolder.updateVoiceState(CentralVoiceState.LISTENING)
        // Must still remain EXECUTING until task finishes
        assertEquals(EdgeGlowState.EXECUTING, EdgeGlowStateHolder.glowState.value)
    }

    // 6. Persistent Dashboard Voice Mode
    @Test
    fun testPersistentDashboardVoiceMode() {
        EdgeGlowStateHolder.setPersistentVoiceMode(true)
        assertTrue(EdgeGlowStateHolder.isPersistentVoiceModeActive.value)
        assertEquals(EdgeGlowState.LISTENING, EdgeGlowStateHolder.glowState.value)

        // Speaking transition
        EdgeGlowStateHolder.updateVoiceState(CentralVoiceState.SPEAKING, currentSoundLevel = 0.5f)
        assertEquals(EdgeGlowState.SPEAKING, EdgeGlowStateHolder.glowState.value)

        // When speech finishes, persistent voice mode returns to LISTENING instead of IDLE
        EdgeGlowStateHolder.updateVoiceState(CentralVoiceState.IDLE)
        assertEquals(EdgeGlowState.LISTENING, EdgeGlowStateHolder.glowState.value)

        // Turn OFF persistent voice mode from dashboard
        EdgeGlowStateHolder.setPersistentVoiceMode(false)
        assertFalse(EdgeGlowStateHolder.isPersistentVoiceModeActive.value)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
    }

    // 7. AI Tool Registry and Execution: maps and executes requested tools
    @Test
    fun testAiToolRegistryExecution() = runBlocking {
        val registry = AiToolRegistry(application)

        // Verify battery tool matching
        val batteryTool = registry.findMatchingTool("check battery status")
        assertNotNull(batteryTool)
        assertEquals("check battery status", batteryTool!!.name)

        val batteryResult = registry.executeTool(batteryTool)
        assertTrue(batteryResult.isSuccess)
        assertTrue(batteryResult.message.contains("Battery"))

        // Verify Memos tool
        val memoTool = registry.findMatchingTool("add to-do buy groceries")
        assertNotNull(memoTool)
        assertEquals("add to-do", memoTool!!.name)

        val memoResult = registry.executeTool(memoTool, mapOf("todo" to "buy groceries"))
        assertTrue(memoResult.isSuccess)
        assertTrue(memoResult.message.contains("groceries"))

        // Verify Date & Time tool
        val timeTool = registry.findMatchingTool("what time is it")
        assertNotNull(timeTool)
        val timeResult = registry.executeTool(timeTool!!)
        assertTrue(timeResult.isSuccess)
        assertTrue(timeResult.message.contains("Date & Time"))
    }

    // 8. Visual theme and colors for all states
    @Test
    fun testEdgeGlowColorMappingsForAllStates() {
        val colorOption = EdgeGlowColorOption.AUTO

        for (state in EdgeGlowState.entries) {
            val colors = colorOption.getColors(state)
            assertTrue("Colors for state $state must not be empty", colors.isNotEmpty())
        }

        val successColors = colorOption.getColors(EdgeGlowState.SUCCESS)
        assertTrue(successColors.isNotEmpty())

        val waitingColors = colorOption.getColors(EdgeGlowState.WAITING_FOR_CONFIRMATION)
        assertTrue(waitingColors.isNotEmpty())

        val failedColors = colorOption.getColors(EdgeGlowState.FAILED)
        assertTrue(failedColors.isNotEmpty())

        val cancelledColors = colorOption.getColors(EdgeGlowState.CANCELLED)
        assertTrue(cancelledColors.isNotEmpty())
    }
}
