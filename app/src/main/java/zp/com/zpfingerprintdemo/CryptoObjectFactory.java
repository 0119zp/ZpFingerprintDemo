package zp.com.zpfingerprintdemo;

import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

import java.security.Key;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

/**
 * Created by Administrator on 2018/3/4 0004.
 */
public class CryptoObjectFactory {

    // This can be key name you want. Should be unique for the app.
    private static final String KEY_NAME = "com.pinganfang.haofangtuo";

    // We always use this keystore on Android.
    private static final String KEYSTORE_NAME = "AndroidKeyStore";

    // Should be no need to change these values.
    private static final String KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    private static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    private static final String ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    private static final String TRANSFORMATION = KEY_ALGORITHM + "/" + BLOCK_MODE + "/" + ENCRYPTION_PADDING;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static FingerprintManager.CryptoObject buildCryptoObject() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_NAME);
        keyStore.load(null);
        Cipher cipher = createCipher(keyStore, true);
        return new FingerprintManager.CryptoObject(cipher);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static Cipher createCipher(KeyStore keyStore, boolean retry) throws Exception {
        Key key = GetKey(keyStore);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        try {
            cipher.init(Cipher.ENCRYPT_MODE | Cipher.DECRYPT_MODE, key);
        } catch (KeyPermanentlyInvalidatedException e) {
            keyStore.deleteEntry(KEY_NAME);
            if (retry) {
                createCipher(keyStore, false);
            } else {
                throw new Exception("Could not create the cipher for fingerprint authentication.", e);
            }
        }
        return cipher;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static Key GetKey(KeyStore keyStore) throws Exception {
        Key secretKey;
        // create key
        if (!keyStore.isKeyEntry(KEY_NAME)) {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM, KEYSTORE_NAME);
            KeyGenParameterSpec keyGenSpec = new KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(BLOCK_MODE)
                    .setEncryptionPaddings(ENCRYPTION_PADDING)
                    .setUserAuthenticationRequired(true)
                    .build();
            keyGen.init(keyGenSpec);
            keyGen.generateKey();
        }

        secretKey = keyStore.getKey(KEY_NAME, null);
        return secretKey;
    }

}
