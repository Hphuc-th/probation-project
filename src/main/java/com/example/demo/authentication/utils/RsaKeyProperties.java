package com.example.demo.authentication.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt.rsa")
public class RsaKeyProperties {

    private static final String KEYS_DIR = "keys";
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PRIVATE_KEY_FILE = "private.key";

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(RSAPrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @jakarta.annotation.PostConstruct
    public void loadOrGenerateKeys() {
        File dir = new File(KEYS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File pubFile = new File(dir, PUBLIC_KEY_FILE);
        File privFile = new File(dir, PRIVATE_KEY_FILE);

        try {
            if (pubFile.exists() && privFile.exists()) {
                publicKey = loadPublicKey(Files.readAllBytes(pubFile.toPath()));
                privateKey = loadPrivateKey(Files.readAllBytes(privFile.toPath()));
            } else {
                generateAndSaveKeys(pubFile, privFile);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load or generate RSA keys", e);
        }
    }

    private void generateAndSaveKeys(File pubFile, File privFile) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        try (FileOutputStream pubOut = new FileOutputStream(pubFile);
             FileOutputStream privOut = new FileOutputStream(privFile)) {
            pubOut.write(Base64.getEncoder().encodeToString(publicKey.getEncoded()).getBytes());
            privOut.write(Base64.getEncoder().encodeToString(privateKey.getEncoded()).getBytes());
        }
    }

    private RSAPublicKey loadPublicKey(byte[] keyBytes) throws Exception {
        String key = new String(keyBytes).replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }

    private RSAPrivateKey loadPrivateKey(byte[] keyBytes) throws Exception {
        String key = new String(keyBytes).replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    }
}
