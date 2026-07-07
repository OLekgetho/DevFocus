package com.devfocus.auth.service;

import com.devfocus.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest{

    private String testKey;
    private String testKey2;
    private EncryptionService encryptionService;
    private EncryptionService encryptionService2;

    @BeforeEach
    void setUp() {
        byte[] randomBytes = new byte[32];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomBytes);

        this.testKey = Base64.getEncoder().encodeToString(randomBytes);

        byte[] randomBytes2 = new byte[32];

        SecureRandom secureRandom2 = new SecureRandom();
        secureRandom2.nextBytes(randomBytes2);
        this.testKey2 = Base64.getEncoder().encodeToString(randomBytes2);


        encryptionService = new EncryptionService(testKey);
        encryptionService2 = new EncryptionService(testKey2);

    }


    @Test
    void encryptThenDecryptReturnsOriginalPlaintext() {
        String sampleText = "oasahi3280skasboaw";
        String encrypted = encryptionService.encrypt(sampleText);

        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(sampleText);
    }

    @Test
    void encryptingSamePlaintextTwiceProducesDifferentCiphertexts() {
        String sampleText = "oasahi3280skasboaw";
        String encryptedText1 = encryptionService.encrypt(sampleText);
        String encryptedText2 = encryptionService.encrypt(sampleText);

        String decryptedText1 = encryptionService.decrypt(encryptedText1);
        String decryptedText2 = encryptionService.decrypt(encryptedText2);

        assertThat(encryptedText1).isNotEqualTo(encryptedText2);
        assertThat(decryptedText1).isEqualTo(decryptedText2);
    }

    @Test
    @DisplayName("decrypting tampered ciphertext throws AppException")
    void decryptingTamperedCiphertextThrows() {
        String sampleText = "oasahi3280skasboaw";
        String encryptedText = encryptionService.encrypt(sampleText);

        StringBuilder sb = new StringBuilder(encryptedText);
        int middleIndex = sb.length() / 2;

        sb.setCharAt(middleIndex, 'O');
        String tamperedText = sb.toString();

        assertThatThrownBy(() -> encryptionService.decrypt(tamperedText))
                .isInstanceOf(AppException.class);
    }

    @Test
    void emptyStringRoundTripsSuccessfully() {
        String sampleText = "";
        String encryptedText = encryptionService.encrypt(sampleText);
        String decryptedText = encryptionService.decrypt(encryptedText);
        assertThat(decryptedText).isEqualTo(sampleText);

    }

    @Test
    void unicodePlaintextRoundTripsSuccessfully() {
        String sampleText = "überdev 🚀 セキュリティ";
        String encryptedText = encryptionService.encrypt(sampleText);
        String decryptedText = encryptionService.decrypt(encryptedText);
        assertThat(decryptedText).isEqualTo(sampleText);
    }

    @Test
    void decryptingInvalidBase64Throws() {
        String sampleText = "not-even-base64!!!";
        assertThatThrownBy(() -> encryptionService.decrypt(sampleText))
                .isInstanceOf(AppException.class);
    }

    @Test
    void decryptingWithDifferentKeyThrows() {
        String sampleText = "Hi to anyone reading this";
        String encryptedText = encryptionService.encrypt(sampleText);

        assertThatThrownBy(() -> encryptionService2.decrypt(encryptedText))
                .isInstanceOf(AppException.class);
    }

}