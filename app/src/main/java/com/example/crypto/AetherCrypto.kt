package com.example.crypto

import android.util.Base64
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AetherCrypto {

    // Generate EC KeyPair (ECDH over prime256v1 / secp256r1)
    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    fun publicKeyToBase64(pubKey: PublicKey): String {
        return Base64.encodeToString(pubKey.encoded, Base64.NO_WRAP)
    }

    fun privateKeyToBase64(privKey: PrivateKey): String {
        return Base64.encodeToString(privKey.encoded, Base64.NO_WRAP)
    }

    fun base64ToPublicKey(base64: String): PublicKey {
        val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(X509EncodedKeySpec(keyBytes))
    }

    fun base64ToPrivateKey(base64: String): PrivateKey {
        val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePrivate(PKCS8EncodedKeySpec(keyBytes))
    }

    // ECDH Key Agreement to derive symmetric shared secret
    fun deriveSharedSecret(ourPrivateBase64: String, peerPublicBase64: String): ByteArray {
        val ourPrivate = base64ToPrivateKey(ourPrivateBase64)
        val peerPublic = base64ToPublicKey(peerPublicBase64)

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(ourPrivate)
        keyAgreement.doPhase(peerPublic, true)
        val rawSecret = keyAgreement.generateSecret()

        // Hash the secret to derive a high-entropy 256-bit symmetric key
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(rawSecret)
    }

    // Encrypt string with AES-GCM
    fun encrypt(plainText: String, secretKey: ByteArray): String {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            // Use standard GCM IV size: 12 bytes
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(128, iv)
            // Use 128-bit key derived from the first 16 bytes of the shared secret
            val keySpec = SecretKeySpec(secretKey.copyOf(16), "AES")
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Output layout: IVFingerprint (12 bytes) + CipherText (N bytes)
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return "[ENCRYPTION_ERROR]"
        }
    }

    // Decrypt string with AES-GCM
    fun decrypt(cipherTextBase64: String, secretKey: ByteArray): String {
        try {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            if (combined.size < 12) return "[DECRYPTION_MALFORMED]"
            
            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            
            val cipherText = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, cipherText, 0, cipherText.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            val keySpec = SecretKeySpec(secretKey.copyOf(16), "AES")
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            val plainBytes = cipher.doFinal(cipherText)
            return String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return "[CORRUPTED_CIPHERTEXT]"
        }
    }

    // SHA-256 fingerprint for identity / verification key representation
    fun generateFingerprint(publicKeyBase64: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(publicKeyBase64.toByteArray(Charsets.UTF_8))
            // Take first 4 bytes of hash and format beautifully as Hex
            val block1 = hash.copyOfRange(0, 2).joinToString("") { "%02X".format(it) }
            val block2 = hash.copyOfRange(2, 4).joinToString("") { "%02X".format(it) }
            "AE-$block1-$block2"
        } catch (e: Exception) {
            "AE-ERROR"
        }
    }
}
