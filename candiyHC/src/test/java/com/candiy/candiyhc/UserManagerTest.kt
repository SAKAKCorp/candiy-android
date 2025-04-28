//package com.candiy.candiyhc
//
//import com.candiy.candiyhc.network.ApiService
//import kotlinx.coroutines.runBlocking
//import okhttp3.mockwebserver.MockResponse
//import okhttp3.mockwebserver.MockWebServer
//import okhttp3.mockwebserver.RecordedRequest
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import retrofit2.HttpException
//import retrofit2.Response
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import kotlin.test.assertEquals
//import kotlin.test.assertFailsWith
//
//class UserManagerTest {
//
//    private lateinit var mockWebServer: MockWebServer
//    private lateinit var apiService: ApiService
//    private lateinit var userManager: UserManager
//
//    @Before
//    fun setUp() {
//        mockWebServer = MockWebServer()
//        mockWebServer.start()
//
//        val retrofit = Retrofit.Builder()
//            .baseUrl(mockWebServer.url("/"))
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        apiService = retrofit.create(ApiService::class.java)
//        userManager = UserManager(apiService, mockWebServer.context) // userManager는 context를 받는다고 가정
//    }
//
//    @After
//    fun tearDown() {
//        mockWebServer.shutdown()
//    }
//
//    @Test
//    fun testValidateOrCreateUser_Success() = runBlocking {
//        // Mock response for getUserOauthInfo
//        mockWebServer.enqueue(
//            MockResponse()
//            .setResponseCode(200)
//            .setBody("""
//                {
//                    "user": {"id": "1", "name": "John Doe"},
//                    "uid": "testUid",
//                    "secret": "testSecret"
//                }
//            """))
//
//        // Mock response for getAccessToken
//        mockWebServer.enqueue(MockResponse()
//            .setResponseCode(200)
//            .setBody("""
//                {
//                    "access_token": "mockAccessToken",
//                    "token_type": "bearer",
//                    "expires_in": 3600
//                }
//            """))
//
//        // Test the function
//        val token = userManager.validateOrCreateUser("testEndUserId")
//
//        // Assert that the correct token is returned
//        assertEquals("mockAccessToken", token)
//
//        // Verify that the correct API endpoint was hit
//        val recordedRequest: RecordedRequest = mockWebServer.takeRequest()
//        assertEquals("/api/user/testEndUserId/user_oauth_info", recordedRequest.path)
//    }
//
//    @Test
//    fun testValidateOrCreateUser_Fail_WithoutUser() = runBlocking {
//        // Mock response for getUserOauthInfo with failure
//        mockWebServer.enqueue(MockResponse()
//            .setResponseCode(404)
//            .setBody("{}"))
//
//        // Mock response for createUser
//        mockWebServer.enqueue(MockResponse()
//            .setResponseCode(200)
//            .setBody("""
//                {
//                    "user": {"id": "1", "name": "John Doe"},
//                    "uid": "testUid",
//                    "secret": "testSecret"
//                }
//            """))
//
//        // Mock response for getAccessToken
//        mockWebServer.enqueue(MockResponse()
//            .setResponseCode(200)
//            .setBody("""
//                {
//                    "access_token": "mockAccessToken",
//                    "token_type": "bearer",
//                    "expires_in": 3600
//                }
//            """))
//
//        // Test the function
//        val token = userManager.validateOrCreateUser("testEndUserId")
//
//        // Assert that the correct token is returned
//        assertEquals("mockAccessToken", token)
//    }
//
//    @Test
//    fun testFetchToken_Fail(): Unit = runBlocking {
//        // Test fetchToken failure by mocking an error response
//        mockWebServer.enqueue(MockResponse()
//            .setResponseCode(400)
//            .setBody("""
//                {
//                    "error": "invalid_grant",
//                    "error_description": "Invalid credentials"
//                }
//            """))
//
//        assertFailsWith<Exception> {
//            userManager.fetchToken("invalidUid", "invalidSecret")
//        }
//    }
//}
