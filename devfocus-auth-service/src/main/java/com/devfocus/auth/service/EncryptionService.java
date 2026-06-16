package com.devfocus.auth.service;

import com.devfocus.shared.constants.ErrorCode;
import com.devfocus.shared.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {

    private final String encryptionKey;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    public EncryptionService(@Value("${encryption.key}")  String encryptionKey ) {
        this.encryptionKey = encryptionKey;
    }

    public String encrypt(String plainText) {

        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            byte[] keyBytes = Base64.getDecoder().decode(this.encryptionKey);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE,secretKeySpec,new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];

            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception ex) {

            throw new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Encryption Failed");
        }
    }


    public String decrypt(String encryptedText) {

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);

            byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            byte[] keyBytes = Base64.getDecoder().decode(this.encryptionKey);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE,secretKeySpec,new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] plainBytes = cipher.doFinal(ciphertext);

            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (Exception ex) {

            throw new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Decryption Failed");
        }
    }
}
