import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
	private UTXOPool transLedger;
	private ArrayList<Transaction> validTxs = new ArrayList<Transaction>();

    public TxHandler(UTXOPool utxoPool) {
    	this.transLedger  = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx}, (if claimed multiple times => exists more than once in inputs)
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
    	ArrayList<Transaction.Output> outputs = tx.getOutputs();
    	ArrayList<Transaction.Input> inputs = tx.getInputs();
    	UTXOPool tempPool = new UTXOPool(transLedger);	//copy for solving element uniqueness across UTXO claims
    	UTXO tempUTXO;
    	double inVal = 0.0;
    	double outVal = 0.0;
    	Transaction.Input tempIn;
    	Transaction.Output tempOut;

    	 // Solving (1), (2), (3)
    	for (int i=0; i < inputs.size(); i++) {
    		tempIn = inputs.get(i);
    		tempUTXO = new UTXO (tempIn.prevTxHash, tempIn.outputIndex); //grabbing the UTXO from which this input stems
    		if (!tempPool.contains(tempUTXO)) return false;	//UTXO not in pool => null claim	OR UTXO already claimed in transaction
    		tempOut = tempPool.getTxOutput(tempUTXO); // the address and index of the prev transaction that should lead into our input
    		if (tempOut == null) return false;
    		if (!Crypto.verifySignature(tempOut.address, tx.getRawDataToSign(i), tempIn.signature)) return false;
    		inVal += tempOut.value;
    		tempPool.removeUTXO(tempUTXO);
    	}

    	 // Solving (4), (5)
    	int j = 0;
    	while (j < outputs.size()){
    		tempOut = outputs.get(j);
    		outVal = (tempOut.value > 0.0) ? outVal + tempOut.value : -1.0; //if the value is negative then false, else add
    		if (outVal < 0.0) return false;
    		j += 1;
    	}
    	if (outVal > inVal) return false;

    	return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     * @param possibleTxs
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

    	ArrayList<Transaction> possibleDependantTxs = new ArrayList<Transaction>();
    	for (Transaction tx: possibleTxs) {
    		if (isValidTx(tx) && !this.validTxs.contains(tx)) {
    			this.addToPool(tx);
    			this.validTxs.add(tx);
    		}
    		else possibleDependantTxs.add(tx);
    	}

    	/**
    	 * Note:
			 * Since some transactions are dependant on other transactions in possibleTxs, I tried
    	 * re-verifying any transactions that failed on the first verification loop.
    	 * Pretty sure this idea didn't work but I ran out of time
    	 */

    	for (Transaction tx: possibleDependantTxs) {
    		if (isValidTx(tx) && !this.validTxs.contains(tx)) {
    			this.addToPool(tx);
    			this.validTxs.add(tx);
    		}
    	}

    	Transaction addTxs[] = new Transaction[this.validTxs.size()];
    	return this.validTxs.toArray(addTxs);

    }

    /**
     * Helper function to add and remove UTXO's from the pool for each (valid) transaction
     * @param tx
     */

    private void addToPool(Transaction tx) {
	   	tx.finalize();
	   	// First removing the input UTXO as it is spent
	   	ArrayList<Transaction.Input> inputs = tx.getInputs();
	   	UTXO t;
			for (Transaction.Input input : inputs) {
				t = new UTXO(input.prevTxHash, input.outputIndex);
				if (transLedger.contains(t)) this.transLedger.removeUTXO(t);
			}
			// Then adding the new UTXO
			int i = 0;	//serial num
			ArrayList<Transaction.Output> outputs = tx.getOutputs();
			for (Transaction.Output output: outputs) {
				t = new UTXO(tx.getHash(), i);
				if (!transLedger.contains(t)) this.transLedger.addUTXO(t, output);
				i++;
			}
   }

}
