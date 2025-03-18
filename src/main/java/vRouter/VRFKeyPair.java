package vRouter;

import java.security.*;

public class VRFKeyPair {
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public VRFKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC"); // 椭圆曲线
            keyGen.initialize(256); // secp256r1 曲线
            KeyPair keyPair = keyGen.generateKeyPair();
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密钥生成失败", e);
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
