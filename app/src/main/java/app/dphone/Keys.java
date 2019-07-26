package app.dphone;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.security.SecureRandom;


public class Keys {
    private static X9ECParameters curve = SECNamedCurves.getByName("secp256k1");
    private static ECDomainParameters domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(), curve.getH());
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String GetPublicKey(String privateKey)
    {
        BigInteger d = new BigInteger(privateKey, 16);
        ECPoint q = domain.getG().multiply(d);
        return bytesToHex(q.getEncoded());
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String Sign(String message, String privateKey) throws Exception
    {
        ECPrivateKeyParameters privateKeySpec = new ECPrivateKeyParameters (new BigInteger(privateKey, 16), domain);
        ParametersWithRandom param = new ParametersWithRandom(privateKeySpec, new SecureRandom());
        ECDSASigner signer = new ECDSASigner();
        signer.init(true, param);
        BigInteger[] signature = signer.generateSignature(message.getBytes("UTF-8"));
        return signature[0].toString(16) + "." + signature[1].toString(16);
    }

    public static boolean VerifySignature(String message, String publicKey, String signature)
    {
        try {
            String[] signatureParts = signature.split("\\.");
            if (signatureParts.length != 2) {
                return false;
            }
            ECPublicKeyParameters publicKeyParams = new ECPublicKeyParameters(curve.getCurve().decodePoint(hexToBytes(publicKey)), domain);
            ECDSASigner signer = new ECDSASigner();
            signer.init(false, publicKeyParams);
            return signer.verifySignature(message.getBytes("UTF-8"), new BigInteger(signatureParts[0], 16), new BigInteger(signatureParts[1], 16));
        } catch (Exception e) {
            return false;
        }
    }
}
