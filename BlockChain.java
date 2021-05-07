import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory
// as it would cause a memory overflow.

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private HashMap<ByteArrayWrapper, blockInfo> chain;

    private TransactionPool trxns;
    private byte[] maxHeightHash;	//hash of the max height block
    private int numBlocks = 0;	//used for getting age
    public blockInfo max_height_bI;

    private ByteArrayWrapper w(byte[] arr) {
        return new ByteArrayWrapper(arr);
    }

    /*
    linked list style nodes to match our tree-ish structure
    */
    public class blockInfo {
    	public Block block;
    	public blockInfo previous; //parent block
    	private ArrayList<blockInfo> outputs; //linked list style tree structure, keep track of child blocks
    	public int height;
    	public int age;
    	private UTXOPool pool;

    	public blockInfo(Block b, int age, blockInfo prev, UTXOPool pool) {
    		  block = b;
    		  outputs = new ArrayList<>();
      		previous = prev;
      		height = (prev != null) ? prev.height + 1: 1; //if genesis block, height is 1
      		if (prev != null) prev.addOut(this);
      		this.age = age;
      		this.pool = pool;
    	}
    	public void addOut(blockInfo b) {
    		outputs.add(b);
    	}
    	}
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
    	chain = new HashMap<>();
    	numBlocks += 1;
    	UTXOPool gPool = new UTXOPool();
    	blockInfo g_bI = new blockInfo(genesisBlock, numBlocks, null, gPool);
    	max_height_bI = g_bI;
    	chain.put(w(genesisBlock.getHash()), g_bI);
    	trxns = new TransactionPool();
    }

    public boolean pruneOldBlocks(Block oldestBlock) {
    	//for each block behind the oldestBlock, remove them from the hashmaps blockTree, blockHeight, blockAge
    	return true;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
    	return max_height_bI.block;
    }

    // Gets the integer value of the function above
    public int getMaxHeightInt() {
    	return max_height_bI.height;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
    	return max_height_bI.pool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
    	return trxns;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     *
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
    	//validate trxns
    	byte[] prevHash = block.getPrevBlockHash();		//hash of the input block
    	if (prevHash == null || !chain.containsKey(w(prevHash))) return false;		//false genesis block or non existent parent
    	TxHandler txh = new TxHandler(new UTXOPool (chain.get(w(prevHash)).pool));
    	Transaction[] block_trxns = block.getTransactions().toArray(new Transaction[0]);
    	Transaction[] valid_trxns = txh.handleTxs(block_trxns);
    	UTXOPool p = txh.getUTXOPool();
    	Transaction cb = block.getCoinbase();
    	addToPool(cb, p);

    	blockInfo prevBlock = chain.get(w(prevHash));	   //info for the input block
    	int prevHeight = prevBlock.height;				       //height of the input block
    	int maxHeight = getMaxHeightInt();				       //current height for the top of the chain
    	if (prevHeight + 1 > maxHeight - CUT_OFF_AGE) {
	    	numBlocks += 1;
	    	blockInfo new_bI = new blockInfo(block, numBlocks, prevBlock, p);
	    	chain.put(w(block.getHash()), new_bI);
	    	max_height_bI = (new_bI.height > max_height_bI.height) ? new_bI : max_height_bI;
	    	return true;
    	}
    	return false;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
    	trxns.addTransaction(tx);
    }

    //From Assignment 1
    private void addToPool(Transaction tx, UTXOPool p) {
		int i = 0;
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		for (Transaction.Output output: outputs) {
			UTXO t = new UTXO(tx.getHash(), i);
			if (!p.contains(t)) p.addUTXO(t, output);
			i++;
		}
   }
}
