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
public class SplitBalancing extends PathSelection {

    public SplitBalancing(DistanceFunction df) {
        super("SPLIT_BALANCING", df);
    }

    @Override
    public void initRoutingInfo(Graph g, Random rand) {
        this.dist.initRouteInfo(g, rand);

    }


    @Override
    public double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded,
                                 RoutePayment rp, double curVal,
                                 Random rand, int reality) {
        Node[] nodes = g.getNodes();
        int[] out = nodes[cur].getOutgoingEdges();
        //sum all funds that can be forwarded
        double sum = 0;
        double minimal = 0.75;
        HashMap<Double, Vector<Integer>> pots = new HashMap<Double, Vector<Integer>>();
        HashMap<Double, Vector<Integer>> balances = new HashMap<>();
        for (int k = 0; k < out.length; k++) {
            if (out[k] == pre || excluded[out[k]]) continue;
            if (this.dist.isCloser(out[k], cur, dst, reality)) {
                double pot = rp.computePotential(cur, out[k]);

                double totalcap = rp.getTotalCapacity(cur, out[k]);
                double capp = (totalcap - pot) / totalcap;
                if(capp > 0.9) {
                    Vector<Integer> v = balances.get(capp);
                    if (v == null) {
                        v = new Vector<Integer>();
                        balances.put(capp, v);
                    }

                    balances.put((totalcap - pot)/totalcap, v);
                    v.add(k);
                }


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
            it = balances.keySet().iterator();
            double[] balancesArray = new double[balances.size()];
            c = 0;
            while (it.hasNext()) {
                balancesArray[c] = it.next();
                c++;
            }
            Arrays.sort(vals);
            Arrays.sort(balancesArray);

            double all = 0; //already assigned funds
            for(int i = balancesArray.length - 1; i >= 0; i--) {
                if(balancesArray[i] > minimal && !(all >= curVal)) {
                    Vector<Integer> vec = balances.get(balancesArray[i]);

                    while(vec.size() > 0) {
                        int node = vec.remove(rand.nextInt(vec.size()));
                        double valNode = Math.min(curVal - all, rp.getTotalCapacity(cur, out[node]) * (balancesArray[i] - 0.5));
                        all += valNode;
                        partVal[node] = valNode;
                        if(all >= curVal) break;
                    }

                } else break;
            }
            //iteratively assign funds to be forwarded to neighors
            for (int i = vals.length-1; i > -1; i--) {
                //start with nodes with highest funds to reduce splitting
                Vector<Integer> vec = pots.get(vals[i]);
                while (vec.size() > 0) {
                    int node = vec.remove(rand.nextInt(vec.size()));
                    //assign as many funds as possible to this node: all remaining funds if possible, otherwise maximum they can forward
                    double valNode = Math.min(rp.computePotential(cur, out[node]), curVal-all);
                    all = all + valNode;
                    partVal[node] += valNode;
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
            return partVal;
        }

    }

}
