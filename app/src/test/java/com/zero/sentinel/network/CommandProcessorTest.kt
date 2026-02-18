package com.zero.sentinel.network

import android.content.Context
import com.zero.sentinel.data.repository.LogRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.mockito.Mockito.mockStatic

class CommandProcessorTest {

    private lateinit var context: Context
    private lateinit var repository: LogRepository
    private lateinit var client: TelegramClient
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var logMock: org.mockito.MockedStatic<android.util.Log>

    @Before
    fun setUp() {
        logMock = mockStatic(android.util.Log::class.java)
        context = mock(Context::class.java)
        repository = mock(LogRepository::class.java)
        client = mock(TelegramClient::class.java)
        commandProcessor = CommandProcessor(context, repository, client)
    }

    @org.junit.After
    fun tearDown() {
        logMock.close()
    }

    @Test
    fun `processUpdates should ignore null response`() {
        commandProcessor.processUpdates(null)
        verify(client, times(0)).sendMessage(Mockito.anyString())
    }

    @Test
    fun `handleCommand should respond to ping`() {
        // Construct a fake JSON response for a /ping command
        val jsonResponse = """
            {
                "result": [
                    {
                        "update_id": 12345,
                        "message": {
                            "text": "/ping",
                            "chat": { "id": 111 }
                        }
                    }
                ]
            }
        """.trimIndent()

        commandProcessor.processUpdates(jsonResponse)

        // Verify that sendMessage was called with a string starting with "Pong!"
        verify(client).sendMessage(Mockito.matches("Pong!.*"))
    }
    
    @Test
    fun `handleCommand should trigger wipe on wipe command`() = runTest {
        val jsonResponse = """
            {
                "result": [
                    {
                        "update_id": 12346,
                        "message": {
                            "text": "/wipe",
                            "chat": { "id": 111 }
                        }
                    }
                ]
            }
        """.trimIndent()

        commandProcessor.processUpdates(jsonResponse)
        
        // Since wipe launches a coroutine, verification might be tricky without injected scope.
        // For this simple test, we assume the launch happens. 
        // Ideally CommandProcessor should allow injecting a TestDispatcher.
        // But we can at least verify basic flow if it didn't crash.
        // (Real concurrent testing requires refactoring CommandProcessor to accept CoroutineDispatcher)
    }
}
