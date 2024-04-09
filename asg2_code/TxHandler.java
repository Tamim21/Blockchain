import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is
     * {@code utxoPool}.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     *         (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     *         (2) the signatures on each input of {@code tx} are valid,
     *         (3) no UTXO is claimed multiple times by {@code tx},
     *         (4) all of {@code tx}s output values are non-negative, and
     *         (5) the sum of {@code tx}s input values is greater than or equal to
     *         the sum of its output
     *         values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        double sum_input = 0.0;
        double sum_output = 0.0;
        Set<UTXO> utxoSet = new HashSet<UTXO>();
        for (Transaction.Output tx_output : tx.getOutputs()) {
            if (tx_output.value < 0)
                return false;
            sum_output += tx_output.value;
        }

        ArrayList<Transaction.Input> tx_inputs = tx.getInputs();
        for (Transaction.Input tx_input : tx_inputs) {
            byte[] prevTxHash = tx_input.prevTxHash;
            int outputIndex = tx_input.outputIndex;
            UTXO utxo = new UTXO(prevTxHash, outputIndex);
            utxoSet.add(utxo);
            if (!utxoPool.contains(utxo))
                return false;
            sum_input += utxoPool.getTxOutput(utxo).value;
            byte[] signature = tx_input.signature;
            byte[] RawDataToSign = tx.getRawDataToSign(tx_inputs.indexOf(tx_input));
            PublicKey prev_tx_output_address = utxoPool.getTxOutput(utxo).address;
            if (!Crypto.verifySignature(prev_tx_output_address, RawDataToSign, signature))
                return false;
        }

        if (utxoSet.size() != tx_inputs.size())
            return false;

        if (!(sum_input >= sum_output))
            return false;

        // IMPLEMENT THIS
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions,
     * checking each
     * transaction for correctness, returning a mutually valid array of accepted
     * transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> acceptedTxsList = new ArrayList<>(Arrays.asList(possibleTxs));
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                for (Transaction.Input tx_input : tx.getInputs()) {
                    byte[] prevTxHash = tx_input.prevTxHash;
                    int prevOutputIndex = tx_input.outputIndex;
                    UTXO oldUtxo = new UTXO(prevTxHash, prevOutputIndex);
                    utxoPool.removeUTXO(oldUtxo);

                }
                for (Transaction.Output tx_output : tx.getOutputs()) {
                    UTXO newUtxo = new UTXO(tx.getHash(), tx.getOutputs().indexOf(tx_output));
                    utxoPool.addUTXO(newUtxo, tx_output);
                }
            } else {
                acceptedTxsList.remove(tx);
            }
        }
        // IMPLEMENT THIS

        Transaction[] acceptedTxsArray = acceptedTxsList.toArray(new Transaction[acceptedTxsList.size()]);
        return acceptedTxsArray;
    }

}
