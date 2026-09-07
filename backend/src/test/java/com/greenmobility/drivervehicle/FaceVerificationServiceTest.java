package com.greenmobility.drivervehicle;

import com.greenmobility.modules.drivervehicle.service.FaceVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

public class FaceVerificationServiceTest {

    private FaceVerificationService faceVerificationService;

    @BeforeEach
    void setUp() {
        faceVerificationService = new FaceVerificationService();
    }

    @Test
    void testCalculateCosineSimilarity_IdenticalVectors() {
        Double[] vector = new Double[]{0.6, 0.8, 0.0};
        double similarity = faceVerificationService.calculateCosineSimilarity(vector, vector);
        assertEquals(1.0, similarity, 0.0001, "Cùng một vector phải có độ tương đồng bằng 1.0");
    }

    @Test
    void testCalculateCosineSimilarity_OrthogonalVectors() {
        Double[] vectorA = new Double[]{1.0, 0.0};
        Double[] vectorB = new Double[]{0.0, 1.0};
        double similarity = faceVerificationService.calculateCosineSimilarity(vectorA, vectorB);
        assertEquals(0.0, similarity, 0.0001, "Hai vector trực giao phải có độ tương đồng bằng 0.0");
    }

    @Test
    void testExtractFaceEmbedding_ConsistentForSameImage() {
        MockMultipartFile imageA = new MockMultipartFile("image", "portrait.jpg", "image/jpeg", "imageContentData".getBytes());
        MockMultipartFile imageB = new MockMultipartFile("image", "portrait.jpg", "image/jpeg", "imageContentData".getBytes());

        Double[] vectorA = faceVerificationService.extractFaceEmbedding(imageA);
        Double[] vectorB = faceVerificationService.extractFaceEmbedding(imageB);

        assertNotNull(vectorA);
        assertNotNull(vectorB);
        assertEquals(512, vectorA.length);
        assertEquals(512, vectorB.length);

        double similarity = faceVerificationService.calculateCosineSimilarity(vectorA, vectorB);
        assertEquals(1.0, similarity, 0.0001, "Cùng một ảnh khuôn mặt phải tạo vector có độ tương đồng 1.0");
        assertTrue(similarity >= FaceVerificationService.SIMILARITY_THRESHOLD);
    }
}
