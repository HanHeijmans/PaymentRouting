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

/**
 * only split if necessary
 * if so: split as few times as possible by using neighbors with highest balances
 * @author mephisto
 *
 */
public class SplitWaterfall extends PathSelection {
    ClosestNeighbor cn;

    public SplitWaterfall(DistanceFunction df) {
        super("SPLIT_WATERFALL", df);
        this.cn = new ClosestNeighbor(df); //ClosestNeighbor is used when not splitting
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
            int index = 0;
            //iteratively assign funds to be forwarded to neighors
            for (int i = vals.length-1; i > -1; i--) {
                //start with nodes with highest funds to reduce splitting
                Vector<Integer> vec = pots.get(vals[i]);
                int amount = vec.size();
                index += amount;
                if(i > 0) {
                    double value = Math.min(curVal - all, vals[i] - vals[i - 1]);
                    for(int j = vals.length - 1; j >= i; j--) {
                        Vector<Integer> v = pots.get(vals[j]);
                        for(Integer in : v) {
                            partVal[in] += value / index;
                            all += value;
                        }
                    }

                } else {
                    double value = curVal - all;
                    for(int j = vals.length - 1; j >= i; j--) {
                        Vector<Integer> v = pots.get(vals[j]);
                        for(Integer in : v) {
                            partVal[in] += value / index;
                            all += value;
                        }
                    }
                }
                if (all >= curVal) {
                    //if all funds assigned, stop
                    break;
                }
            }
            return partVal;
        }

    }

}
