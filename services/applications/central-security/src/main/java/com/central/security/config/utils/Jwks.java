package com.central.security.config.utils;

import com.nimbusds.jose.jwk.RSAKey;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

public class Jwks {
    public static RSAKey generateRsa() {
        KeyPair keyPair = KeyPairGeneratorUtils.generateRsaKey();
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }
}
