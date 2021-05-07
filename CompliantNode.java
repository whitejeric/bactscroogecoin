import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
	private double graph, mal, txDist;
	private int nRounds, cRound;
	private boolean[] follows;
	private Set<Transaction> txPool;
	private Set<Integer> badguys;	//malicious or uncomforming nodes
	private HashMap<Integer, Set<Transaction>> firstReceived;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
    	this.graph = p_graph;
    	this.mal = p_malicious;
    	this.txDist = p_txDistribution;
    	this.nRounds = numRounds;
    	this.cRound = 0;
    	this.firstReceived = new HashMap<>();
    	this.badguys = new HashSet<>();
    }

    public void setFollowees(boolean[] followees) {
    	this.follows = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
    	this.txPool = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
    	Set<Transaction> propose = txPool;
    	return propose;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {

	    HashMap<Integer, Set<Transaction>> received = new HashMap<>();
	    //storing all the transactions relative to the sender for this round

	    for (Candidate c: candidates) {
	    	if (!this.follows[c.sender] || this.badguys.contains(c.sender)) continue;
	    		//sender of c is not a followee or determined earlier to be malicious
	    	if (!received.containsKey(c.sender)) {
	    		Set<Transaction> trxns = new HashSet<>();
	    		received.put(c.sender, trxns);
	        }
	    	if (!received.get(c.sender).contains(c.tx)) received.get(c.sender).add(c.tx);
	    }

	    /*
	     * In the first round (and only the first round) we compile a record of transactions broadcast
	     * to our node in relation to their sender
	     */

	    if (cRound == 0) {
	    	for (int nodeId : received.keySet()) {
	    		if (!this.firstReceived.containsKey(nodeId)) {
        			this.firstReceived.put(nodeId, received.get(nodeId));
                }
	    	}
	    }

	    //add all non-malicious trxns to the pool
	    for (int nodeId : received.keySet()) {
	    	if (!this.badguys.contains(nodeId)) {
	    		for (Transaction tx : received.get(nodeId)) this.txPool.add(tx);
	    	}
	    }

	    for (int i=0; i < this.follows.length; i++) {
	    	if (follows[i] && !received.containsKey(i)) this.badguys.add(i);
	    		//the ith node ghosted us! (an honest friend maintains a correspondence)
	    }

	    //compare the last rounds broadcasts vs. the first rounds
	    if (cRound == nRounds - 1) {
		    for (int i = 0; i < this.follows.length; i++) {
		    	if (this.firstReceived.containsKey(i) && received.containsKey(i)) {
		    		if (this.firstReceived.get(i) == received.get(i)) this.badguys.add(i);
		    			//the ith node has consistently broadcast the exact same set of trxn's => malicious
		        }
		    }
	    }

	    //remove all trxns related to badguys
	    for (int b : badguys) {
	    	if (this.firstReceived.containsKey(b)) {
	    		for (Transaction tx: this.firstReceived.get(b)) this.txPool.remove(tx);
	    		this.firstReceived.remove(b);
	    	}
	    }

    	cRound += 1;
    }
}
