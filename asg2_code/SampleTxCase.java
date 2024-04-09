import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

public class SampleTxCase {

    public static void main(String[] args) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException, NoSuchProviderException {

        // Create an empty pool
        UTXOPool pool = new UTXOPool();

        /*
         * create some transaction tx1, that will have one of its outputs in the
         * pool. We will assume that this one was valid.
         */
        Transaction tx1 = new Transaction();

        /*
         * Let's specify the inputs and outputs of tx1. Since we assume already
         * that tx1 is valid, there is no need really to add a proper input
         * here. Compare that when we define tx2 later.
         */
        tx1.addInput(null, 0);

        /*
         * To add an output to the transaction, we need to have the public key
         * of a recipient.
         *
         * Here we are going to generate a key pair. The public key is going to
         * be used by tx1 to specify the recipient, and the private key will be
         * used by the recipient to sign tx2 in order to spend the money.
         * This generation step should happen on the second side, but this is
         * just simulating everything on one machine)
         */

        KeyPair keyPair1 = generateNewKeyPair();

        // specify an output of value 10, and the public key
        tx1.addOutput(10.0, keyPair1.getPublic());
        // needed to compute the id of tx1
        tx1.finalize();

        // let's add this UTXO to the pool
        pool.addUTXO(new UTXO(tx1.getHash(), 0), tx1.getOutput(0));

        /************** RUNNING TEST CASES ****************/
        System.out.println("case1: " + (case1(tx1, keyPair1, pool) ? "Success" : "Fail"));
        System.out.println("case2: " + (case2(tx1, keyPair1, pool) ? "Success" : "Fail"));

        /*
         * The previous code only checks the validity. To update the
         * pool, your implementation of handleTxs() will be called.
         */

    }

    /*
     * This case tests for 2 mutually exclusive valid sets in the given
     * transactions, with unfavorable ordering and duplicate transactions.
     */
    private static boolean case1(Transaction tx1, KeyPair keyPair1, UTXOPool pool)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException {
        TxHandler txHandler = new TxHandler(pool);

        //
        Transaction tx2 = new Transaction();

        tx2.addInput(tx1.getHash(), 0);

        KeyPair keyPair2 = generateNewKeyPair();
        tx2.addOutput(9.0, keyPair2.getPublic());
        tx2.addOutput(1.0, keyPair2.getPublic());

        byte[] sig1_2 = sign(keyPair1.getPrivate(), tx2.getRawDataToSign(0));
        tx2.addSignature(sig1_2, 0);
        tx2.finalize();

        //
        Transaction tx3 = new Transaction();

        tx3.addInput(tx1.getHash(), 0);

        KeyPair keyPair3 = generateNewKeyPair();
        tx3.addOutput(9.0, keyPair3.getPublic());
        tx3.addOutput(1.0, keyPair3.getPublic());

        byte[] sig1_3 = sign(keyPair1.getPrivate(), tx3.getRawDataToSign(0));
        tx3.addSignature(sig1_3, 0);
        tx3.finalize();

        //
        Transaction tx4 = new Transaction();

        tx4.addInput(tx2.getHash(), 0);

        KeyPair keyPair4 = generateNewKeyPair();
        tx4.addOutput(5.0, keyPair4.getPublic());

        byte[] sig3_4 = sign(keyPair2.getPrivate(), tx4.getRawDataToSign(0));
        tx4.addSignature(sig3_4, 0);
        tx4.finalize();

        //
        Transaction tx5 = new Transaction();

        tx5.addInput(tx4.getHash(), 0);
        tx5.addInput(tx2.getHash(), 1);

        KeyPair keyPair5 = generateNewKeyPair();
        tx5.addOutput(6.0, keyPair5.getPublic());

        byte[] sig2_5 = sign(keyPair2.getPrivate(), tx5.getRawDataToSign(1));
        tx5.addSignature(sig2_5, 1);
        byte[] sig3_5 = sign(keyPair4.getPrivate(), tx5.getRawDataToSign(0));
        tx5.addSignature(sig3_5, 0);
        tx5.finalize();

        //
        Transaction[] txValidArr = txHandler.handleTxs(new Transaction[] { tx2, tx2, tx4, tx3, tx5 });
        if (txValidArr.length == 1 && txValidArr[0].equals(tx3)) {
            return true;
        } else if (txValidArr.length == 3
                && Arrays.asList(txValidArr).contains(tx2)
                && Arrays.asList(txValidArr).contains(tx4)
                && Arrays.asList(txValidArr).contains(tx5)) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * This case tests for invalid signature, invalid output values, and a
     * valid transaction taking 2 inputs from tx A, and 1 input from tx B, where
     * tx A takes 1 input from different tx B output.
     */
    private static boolean case2(Transaction tx1, KeyPair keyPair1, UTXOPool pool)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException {
        TxHandler txHandler = new TxHandler(pool);

        //
        Transaction tx2 = new Transaction();

        tx2.addInput(tx1.getHash(), 0);

        KeyPair keyPair2 = generateNewKeyPair();
        tx2.addOutput(9.0, keyPair2.getPublic());
        tx2.addOutput(1.0, keyPair2.getPublic());

        byte[] sig1_2 = sign(keyPair2.getPrivate(), tx2.getRawDataToSign(0));
        tx2.addSignature(sig1_2, 0);
        tx2.finalize();

        //
        Transaction tx3 = new Transaction();

        tx3.addInput(tx1.getHash(), 0);

        KeyPair keyPair3 = generateNewKeyPair();
        tx3.addOutput(7.0, keyPair3.getPublic());
        tx3.addOutput(5.0, keyPair3.getPublic());

        byte[] sig1_3 = sign(keyPair1.getPrivate(), tx3.getRawDataToSign(0));
        tx3.addSignature(sig1_3, 0);
        tx3.finalize();

        //
        Transaction tx4 = new Transaction();

        tx4.addInput(tx1.getHash(), 0);

        KeyPair keyPair4 = generateNewKeyPair();
        tx4.addOutput(7.0, keyPair4.getPublic());
        tx4.addOutput(-1.0, keyPair4.getPublic());

        byte[] sig1_4 = sign(keyPair1.getPrivate(), tx4.getRawDataToSign(0));
        tx4.addSignature(sig1_4, 0);
        tx4.finalize();

        //
        Transaction tx5 = new Transaction();

        tx5.addInput(tx1.getHash(), 0);

        KeyPair keyPair5_0 = generateNewKeyPair();
        KeyPair keyPair5_1 = generateNewKeyPair();
        tx5.addOutput(8.0, keyPair5_0.getPublic());
        tx5.addOutput(2.0, keyPair5_1.getPublic());

        byte[] sig1_5 = sign(keyPair1.getPrivate(), tx5.getRawDataToSign(0));
        tx5.addSignature(sig1_5, 0);
        tx5.finalize();

        //
        Transaction tx6 = new Transaction();

        tx6.addInput(tx5.getHash(), 0);

        KeyPair keyPair6_0 = generateNewKeyPair();
        KeyPair keyPair6_1 = generateNewKeyPair();
        tx6.addOutput(5.0, keyPair6_0.getPublic());
        tx6.addOutput(3.0, keyPair6_1.getPublic());

        byte[] sig5_6 = sign(keyPair5_0.getPrivate(), tx6.getRawDataToSign(0));
        tx6.addSignature(sig5_6, 0);
        tx6.finalize();

        //
        Transaction tx7 = new Transaction();

        tx7.addInput(tx5.getHash(), 1);
        tx7.addInput(tx6.getHash(), 0);
        tx7.addInput(tx6.getHash(), 1);

        KeyPair keyPair7 = generateNewKeyPair();
        tx7.addOutput(10.0, keyPair7.getPublic());

        byte[] sig5_7 = sign(keyPair5_1.getPrivate(), tx7.getRawDataToSign(0));
        tx7.addSignature(sig5_7, 0);
        byte[] sig6_7_0 = sign(keyPair6_0.getPrivate(), tx7.getRawDataToSign(1));
        tx7.addSignature(sig6_7_0, 1);
        byte[] sig6_7_1 = sign(keyPair6_1.getPrivate(), tx7.getRawDataToSign(2));
        tx7.addSignature(sig6_7_1, 2);
        tx7.finalize();

        //
        Transaction[] txValidArr = txHandler
                .handleTxs(new Transaction[] { tx7, tx2, tx5, tx3, tx4, tx5, tx4, tx6, tx7 });
        if (txValidArr.length == 3
                && Arrays.asList(txValidArr).contains(tx5)
                && Arrays.asList(txValidArr).contains(tx6)
                && Arrays.asList(txValidArr).contains(tx7)) {
            return true;
        } else {
            return false;
        }
    }

    public static KeyPair generateNewKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.genKeyPair();
    }

    public static byte[] sign(PrivateKey privKey, byte[] message)
            throws NoSuchAlgorithmException, SignatureException,
            InvalidKeyException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privKey);
        signature.update(message);
        return signature.sign();
    }

}