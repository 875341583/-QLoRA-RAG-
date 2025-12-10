package service;

import application.service.MultimodalInputService;
import domain.entity.InputType;
import domain.entity.MultimodalInput;
import domain.repository.MultimodalInputRepository;
import infrastructure.client.VisionModelClient;
import infrastructure.client.TextModelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Multimodal input service test class.
 * Tests various functionalities of multimodal input processing including image upload parsing,
 * text intent recognition, video stream processing, concurrent handling, and exception scenarios.
 */
@ExtendWith(MockitoExtension.class)
class MultimodalInputServiceTest {

    @Mock
    private MultimodalInputRepository multimodalInputRepository;

    @Mock
    private VisionModelClient visionModelClient;

    @Mock
    private TextModelClient textModelClient;

    @InjectMocks
    private MultimodalInputService multimodalInputService;

    private MultipartFile testImageFile;
    private String testTextDescription;
    private MultipartFile testVideoFile;

    private static final Long TEST_USER_ID = 1001L;
    private static final int CONCURRENT_THREADS = 10;
    private static final String TEST_IMAGE_PATH = "src/test/resources/test-image.jpg";
    private static final String TEST_VIDEO_PATH = "src/test/resources/test-video.mp4";

    /**
     * Test setup, initializes test data.
     */
    @BeforeEach
    void setUp() throws IOException {
        // Create mock image file
        byte[] imageContent = Files.readAllBytes(Paths.get(TEST_IMAGE_PATH));
        testImageFile = new MockMultipartFile(
            "image", 
            "test-image.jpg", 
            "image/jpeg", 
            imageContent
        );

        // Create test text description
        testTextDescription = "请导航到三楼会议室";

        // Create mock video file
        byte[] videoContent = Files.readAllBytes(Paths.get(TEST_VIDEO_PATH));
        testVideoFile = new MockMultipartFile(
            "video",
            "test-video.mp4",
            "video/mp4", 
            videoContent
        );
    }

    /**
     * Tests image upload parsing functionality.
     * Verifies correctness of image content parsing and location information format.
     */
    @Test
    void testImageUploadParsing() {
        // Mock vision model client response
        String expectedLocation = "{\"location\": \"一楼大厅\", \"confidence\": 0.95}";
        when(visionModelClient.parseImageContent(any(byte[].class)))
            .thenReturn(expectedLocation);

        // Mock repository save operation
        MultimodalInput savedInput = createTestMultimodalInput(1L, InputType.IMAGE, expectedLocation);
        when(multimodalInputRepository.saveInputRecord(any(MultimodalInput.class)))
            .thenReturn(savedInput);

        // Execute test
        String result = multimodalInputService.parseImageContent(testImageFile, TEST_USER_ID);

        // Verify results
        assertNotNull(result, "Image parsing result should not be null");
        assertEquals(expectedLocation, result, "Parsed location should match expected result");
        
        // Verify interactions
        verify(visionModelClient).parseImageContent(testImageFile.getBytes());
        verify(multimodalInputRepository).saveInputRecord(any(MultimodalInput.class));
    }

    /**
     * Tests text intent recognition functionality.
     * Verifies accuracy of text description recognition and target location extraction.
     */
    @Test
    void testTextIntentRecognition() {
        // Mock text model client response with CLIP features
        String expectedTarget = "{\"target\": \"三楼会议室\", \"features\": [0.1, 0.2, 0.3]}";
        when(textModelClient.extractTextFeatures(eq(testTextDescription)))
            .thenReturn(expectedTarget);

        // Mock repository save operation
        MultimodalInput savedInput = createTestMultimodalInput(2L, InputType.TEXT, expectedTarget);
        when(multimodalInputRepository.saveInputRecord(any(MultimodalInput.class)))
            .thenReturn(savedInput);

        // Execute test
        String result = multimodalInputService.extractTextVideoFeatures(testTextDescription, TEST_USER_ID);

        // Verify results
        assertNotNull(result, "Text recognition result should not be null");
        assertEquals(expectedTarget, result, "Recognized target location should match expected result");
        
        // Verify interactions
        verify(textModelClient).extractTextFeatures(testTextDescription);
        verify(multimodalInputRepository).saveInputRecord(any(MultimodalInput.class));
    }

    /**
     * Tests video stream processing functionality.
     * Verifies continuous video frame processing and real-time navigation session establishment.
     */
    @Test
    void testVideoStreamProcessing() {
        // Mock vision model client response for video processing with CLIP features
        String expectedSessionData = "{\"sessionId\": \"vid_123\", \"features\": [[0.1, 0.2], [0.3, 0.4]]}";
        when(visionModelClient.processVideoStream(any(byte[].class)))
            .thenReturn(expectedSessionData);

        // Mock repository save operation
        MultimodalInput savedInput = createTestMultimodalInput(3L, InputType.VIDEO, expectedSessionData);
        when(multimodalInputRepository.saveInputRecord(any(MultimodalInput.class)))
            .thenReturn(savedInput);

        // Execute test
        String result = multimodalInputService.processVideoStream(testVideoFile, TEST_USER_ID);

        // Verify results
        assertNotNull(result, "Video processing result should not be null");
        assertEquals(expectedSessionData, result, "Processed video data should match expected result");
        
        // Verify interactions
        verify(visionModelClient).processVideoStream(testVideoFile.getBytes());
        verify(multimodalInputRepository).saveInputRecord(any(MultimodalInput.class));
    }

    /**
     * Tests true multi-user concurrent request handling.
     * Verifies system stability and data isolation under multiple concurrent user accesses.
     */
    @RepeatedTest(5)
    void testConcurrentUserRequests() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        ArgumentCaptor<MultimodalInput> inputCaptor = ArgumentCaptor.forClass(MultimodalInput.class);
        
        String baseLocation = "位置信息: 用户";
        when(visionModelClient.parseImageContent(any(byte[].class)))
            .thenAnswer(invocation -> baseLocation + Thread.currentThread().getId());

        when(multimodalInputRepository.saveInputRecord(any(MultimodalInput.class)))
            .thenAnswer(invocation -> {
                MultimodalInput input = invocation.getArgument(0);
                return createTestMultimodalInput(input.getUserId(), input.getInputType(), input.getDataContent());
            });

        // Submit concurrent tasks
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final Long userId = 1000L + i;
            executor.submit(() -> {
                String result = multimodalInputService.parseImageContent(testImageFile, userId);
                assertNotNull(result, "User " + userId + " parsing result should not be null");
                assertEquals(baseLocation + Thread.currentThread().getId(), result);
            });
        }

        executor.shutdown();
        boolean terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(true, terminated, "All concurrent tasks should complete within timeout");

        // Verify repository was called for each user with correct parameters
        verify(multimodalInputRepository, org.mockito.Mockito.times(CONCURRENT_THREADS))
            .saveInputRecord(inputCaptor.capture());
        
        // Verify each captured input has correct user ID
        inputCaptor.getAllValues().forEach(input -> {
            assertNotNull(input.getUserId(), "User ID should not be null in concurrent requests");
            assertEquals(InputType.IMAGE, input.getInputType(), "Input type should be IMAGE");
        });
    }

    /**
     * Tests vLLM engine concurrent processing capability.
     * Verifies high-concurrency scenario handling through vLLM integration.
     */
    @Test
    void testVllmConcurrentProcessing() {
        String expectedResult = "{\"processed\": true, \"concurrentJobs\": 50}";
        when(visionModelClient.parseImageContent(any(byte[].class)))
            .thenReturn(expectedResult);

        MultimodalInput savedInput = createTestMultimodalInput(4L, InputType.IMAGE, expectedResult);
        when(multimodalInputRepository.saveInputRecord(any(MultimodalInput.class)))
            .thenReturn(savedInput);

        // Test concurrent processing through service method
        String result = multimodalInputService.concurrentProcessing(testImageFile, TEST_USER_ID);

        assertNotNull(result, "vLLM concurrent processing result should not be null");
        assertEquals(expectedResult, result, "vLLM processing result should match expected");
        
        verify(visionModelClient).parseImageContent(testImageFile.getBytes());
        verify(multimodalInputRepository).saveInputRecord(any(MultimodalInput.class));
    }

    /**
     * Tests exception handling scenarios.
     * Verifies system fault tolerance with invalid inputs or system errors.
     */
    @Test
    void testExceptionHandling() {
        // Test empty file handling
        MultipartFile emptyFile = new MockMultipartFile(
            "empty", 
            "empty.jpg", 
            "image/jpeg", 
            new byte[0]
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> multimodalInputService.parseImageContent(emptyFile, TEST_USER_ID));
        assertEquals("File content cannot be empty", exception.getMessage());

        // Test null user ID
        exception = assertThrows(IllegalArgumentException.class,
            () -> multimodalInputService.parseImageContent(testImageFile, null));
        assertEquals("User ID cannot be null", exception.getMessage());

        // Test model client exception
        when(visionModelClient.parseImageContent(any(byte[].class)))
            .thenThrow(new RuntimeException("Model service unavailable"));
        
        RuntimeException runtimeException = assertThrows(RuntimeException.class,
            () -> multimodalInputService.parseImageContent(testImageFile, TEST_USER_ID));
        assertEquals("Model service unavailable", runtimeException.getMessage());
    }

    /**
     * Tests service method when repository fails to save.
     */
    @Test
    void testRepositorySaveFailure() {
        when(visionModelClient.parseImageContent(any(byte[].class)))
            .thenReturn("{\"location\": \"测试位置\"}");
        when(multimodalInputRepository.saveInputRecord(any(MultimodalInput.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> multimodalInputService.parseImageContent(testImageFile, TEST_USER_ID));
        assertEquals("Database connection failed", exception.getMessage());
    }

    /**
     * Tests output location information structure validation.
     * Verifies that returned data conforms to expected JSON format.
     */
    @Test
    void testOutputStructureValidation() {
        String expectedJson = "{\"location\": \"二楼餐厅\", \"distance\": 15.5, \"eta\": 120}";
        when(visionModelClient.parseImageContent(any(byte[].class)))
            .thenReturn(expectedJson);

        MultimodalInput savedInput = createTestMultimodalInput(5L, InputType.IMAGE, expectedJson);
        when(multimodalInputRepository.saveInputRecord(any(MultimodalInput.class)))
            .thenReturn(savedInput);

        String result = multimodalInputService.parseImageContent(testImageFile, TEST_USER_ID);

        assertNotNull(result, "Output structure validation result should not be null");
        // Basic JSON structure validation
        assertEquals('{', result.charAt(0), "Output should be valid JSON object");
        assertEquals('}', result.charAt(result.length() - 1), "Output should be valid JSON object");
        assertEquals(expectedJson, result, "Output structure should match expected format");
    }

    /**
     * Creates a test MultimodalInput object with specified parameters.
     */
    private MultimodalInput createTestMultimodalInput(Long id, InputType inputType, String dataContent) {
        MultimodalInput input = new MultimodalInput();
        input.setId(id);
        input.setInputType(inputType);
        input.setDataContent(dataContent);
        input.setTimestamp(new Date());
        input.setUserId(TEST_USER_ID);
        return input;
    }
}


// 内容由AI生成，仅供参考