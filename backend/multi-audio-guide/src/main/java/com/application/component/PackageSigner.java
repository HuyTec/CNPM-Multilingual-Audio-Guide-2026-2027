package com.application.component;

import com.application.dto.ContentPackageDto.PublicKey;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;

/** Demo RSA key is process-local; signatures cover the exact UTF-8 package bytes. */
@Component
@Profile("demo")
public class PackageSigner {
    private final KeyPair key;
    public PackageSigner() {
        try { var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); key = generator.generateKeyPair(); }
        catch (Exception error) { throw new IllegalStateException("Cannot initialize demo package signing", error); }
    }
    public PublicKey publicKey() {
        return new PublicKey("RSASSA-PKCS1-v1_5/SHA-256", checksum(key.getPublic().getEncoded()), Base64.getEncoder().encodeToString(key.getPublic().getEncoded()));
    }
    public String sign(byte[] bytes) {
        try {
            Signature signer = Signature.getInstance("SHA256withRSA"); signer.initSign(key.getPrivate()); signer.update(bytes);
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception error) { throw new IllegalStateException("Cannot sign content package", error); }
    }
    public String checksum(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
}
