package paymentrouting.route.split;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import gtna.graph.Graph;
import gtna.graph.Node;
import paymentrouting.route.ClosestNeighbor;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;
import treeembedding.credit.CreditLinks;

public class SplitLookahead extends PathSelection {

    ClosestNeighbor cn;

    public SplitLookahead(DistanceFunction df) {
        super("SPLIT_LOOKAHEAD", df);
        this.cn = new ClosestNeighbor(df);
    }

    @Override
    public void initRoutingInfo(Graph g, Random rand) {
        this.dist.initRouteInfo(g, rand);

    }



    @Override
    public double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded,
                                 RoutePayment rp, double curVal,
                                 Random rand, int reality) {
        //check if not splitting work (using ClosestNeighbor), otherwise go to splitting
        double[] noSplit = this.cn.getNextsVals(g, cur, dst, pre, excluded, rp, curVal, rand, reality);
        if (noSplit != null) {
            return noSplit;
        }
        Node[] nodes = g.getNodes();
        int[] out = nodes[cur].getOutgoingEdges();
        //sum all funds that can be forwarded
        double sum = 0;
        HashMap<Double, Vector<Integer>> pots = new HashMap<Double, Vector<Integer>>();
        for (int k = 0; k < out.length; k++) {
            if (out[k] == pre || excluded[out[k]]) continue;
            if (this.dist.isCloser(out[k], cur, dst, reality)) {
                double pot = rp.computePotential(cur, out[k]);
                Vector<Integer> vec = pots.get(pot);
                if (vec == null) {
                    vec = new Vector<Integer>();
                    pots.put(pot, vec);
                }
                vec.add(k);
                sum = sum + pot;
            }
        }
        if (sum < curVal) {
            //combined balances insufficient -> routing fails
            return null;
        } else {
            //sort nodes by potential (available funds)
            double[] partVal = new double[out.length];
            Iterator<Double> it = pots.keySet().iterator();
            double[] vals = new double[pots.size()];
            int c = 0;
            while (it.hasNext()) {
                vals[c] = it.next();
                c++;
            }
            Arrays.sort(vals);

            double all = 0; //already assigned funds
            //iteratively assign funds to be forwarded to neighors
            for (int i = vals.length-1; i > -1; i--) {
                //start with nodes with highest funds to reduce splitting
                Vector<Integer> vec = pots.get(vals[i]);
                while (vec.size() > 0) {
                    int node = vec.remove(rand.nextInt(vec.size()));
                    //assign as many funds as possible to this node: all remaining funds if possible, otherwise maximum they can forward
                    double lookaheadPotential = Math.min(this.computeTotalPotential(g, out[node], dst, cur, excluded, rp, curVal, rand, reality), vals[i]);
                    double valNode = Math.min(lookaheadPotential, curVal-all);
                    all = all + valNode;
                    partVal[node] = valNode;
                    if (all >= curVal) {
                        //if all funds assigned, stop
                        break;
                    }
                }
                if (all >= curVal) {
                    //if all funds assigned, stop
                    break;
                }
            }
            if(all < curVal) {
                return null;
            }
            return partVal;
        }

    }
    public double computeTotalPotential(Graph g, int cur, int dst, int pre, boolean[] excluded, RoutePayment rp, double curVal,
                                        Random rand, int reality) {
        Node[] nodes = g.getNodes();
        int[] out = nodes[cur].getOutgoingEdges();

        double sum = 0;

        for (int k = 0; k < out.length; k++) {
            if (out[k] == pre || excluded[out[k]]) continue;
            if (this.dist.isCloser(out[k], cur, dst, reality)) {
                sum = sum + rp.computePotential(cur, out[k]);
            }
        }
        return sum;
    }
}
