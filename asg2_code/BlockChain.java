/* I acknowledge that I am aware of the academic integrity
guidelines of this course, and that I worked on this assignment independently
 without any unauthorized help with coding or testing.- <Tamim Sherif> */

/* Additional Excercise
Answer:
Transaction Fees are not included in the CoinBase transaction, the users
inputs that is not used in the transaction are used as transaction fees are not included as
input in the coinbase transaction only the Transaction reward is included in the coinbase transaction.
 */

// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.
import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {

    class Node {
        Block block;
        Node parent;
        ArrayList<Node> parentChildren;
        int Nodeheight;
        UTXOPool utxopool;

        public Node(Block block, Node parent, UTXOPool utxopool) {
            this.utxopool = utxopool;
            this.block = block;
            this.parent = parent;
            parentChildren = new ArrayList<>();
            if (parent != null) {
                Nodeheight = parent.Nodeheight + 1;
                parent.parentChildren.add(this);
            } else
                Nodeheight = 1;
        }

        public UTXOPool copyUtxoPoolByValue() {
            return new UTXOPool(utxopool);
        }
    }

    public static final int CUT_OFF_AGE = 10;
    private TransactionPool txPool;
    private Node maxHeightNode;
    private HashMap<ByteArrayWrapper, Node> blockChain;

    /**
     * create an empty blockchain with just a genesis block. Assume
     * {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        UTXOPool utxoPool = new UTXOPool();
        blockChain = new HashMap<>();
        addCoinbaseTxUtxoPool(genesisBlock, utxoPool);
        Node genesisNode = new Node(genesisBlock, null, utxoPool);
        blockChain.put(wrap(genesisBlock.getHash()), genesisNode);
        txPool = new TransactionPool();
        maxHeightNode = genesisNode;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return maxHeightNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightNode.copyUtxoPoolByValue();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all
     * transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)},
     * where maxHeight is
     * the current height of the blockchain.
     * <p>
     * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e.
     * create a block at
     * height 2) if the current blockchain height is less than or equal to
     * CUT_OFF_AGE + 1. As soon as
     * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a
     * new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        byte[] prevBlockHash = block.getPrevBlockHash();
        Node parentBlockNode = blockChain.get(wrap(prevBlockHash));
        if (prevBlockHash == null || parentBlockNode == null)
            return false;
        TxHandler handler = new TxHandler(parentBlockNode.copyUtxoPoolByValue());
        Transaction[] blockTxs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validBlockTxs = handler.handleTxs(blockTxs);
        int newHeight = parentBlockNode.Nodeheight + 1;
        if (validBlockTxs == null || validBlockTxs.length != blockTxs.length
                || newHeight <= maxHeightNode.Nodeheight - CUT_OFF_AGE)
            return false;
        addCoinbaseTxUtxoPool(block, parentBlockNode.copyUtxoPoolByValue());
        Node node = new Node(block, parentBlockNode, parentBlockNode.copyUtxoPoolByValue());
        blockChain.put(wrap(block.getHash()), node);
        if (newHeight > maxHeightNode.Nodeheight)
            maxHeightNode = node;

        System.gc();

        return true;

    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }

    private static ByteArrayWrapper wrap(byte[] arr) {
        return arr != null ? new ByteArrayWrapper(arr) : null;
    }

    private void addCoinbaseTxUtxoPool(Block block, UTXOPool utxoPool) {
        Transaction coinbaseTx = block.getCoinbase();
        for (Transaction.Output coinbaseOutput : coinbaseTx.getOutputs()) {
            UTXO utxo = new UTXO(coinbaseTx.getHash(), coinbaseTx.getOutputs().indexOf(coinbaseOutput));
            utxoPool.addUTXO(utxo, coinbaseOutput);
        }
    }

}