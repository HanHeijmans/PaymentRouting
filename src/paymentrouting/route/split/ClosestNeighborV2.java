package paymentrouting.route.split;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

import gtna.graph.Graph;
import gtna.graph.Node;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;

/**
 * Splits Ties
 *
 */
public class ClosestNeighborV2 extends PathSelection {

    public ClosestNeighborV2(DistanceFunction d) {
        super("CLOSEST_NEIGHBORV2", d);
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
        double bestDist = Double.MAX_VALUE;
        Vector<Integer> bests = new Vector<Integer>();

        for (int k = 0; k < out.length; k++) {
            //exclude nodes not closer or marked as excluded
            if (out[k] == pre || excluded[out[k]]) continue;
            if (!this.dist.isCloser(out[k], cur, dst, reality)) continue;
            //compute distance
            double d = this.dist.distance(out[k], dst, reality);
            //check if balance/potential of direction is sufficient
            double pot = rp.computePotential(cur, out[k]);
            if (pot >= curVal) {
                if (d < bestDist) {
                    //new neighbor that is closests
                    bests = new Vector<Integer>();
                }
                if (d <= bestDist) {
                    // add to neighbors closests
                    bestDist = d;
                    bests.add(k);
                }
            }
        }
        if (bests.isEmpty()) {
            //routing failed
            return null;
        } else {
            //choose random closests neighbor and forward amount via them
            double[] partVals = new double[out.length];
            double[] balances = new double[out.length];
            double bestBalance = Integer.MIN_VALUE;
            int best = bests.get(0);

            for(Integer i : bests) {
                double capacity = rp.getTotalCapacity(cur,out[i]);
                double balance = (rp.computePotential(cur, out[i]) / capacity) - 0.5;
                double rebalanced = ((rp.computePotential(cur, out[i]) - curVal) / capacity) - 0.5;
                double netBalance = Math.abs(balance) - Math.abs(rebalanced);
//                System.out.println("CAP: " + capacity);
//                System.out.println("Balance: " + rp.computePotential(cur, out[i]));
//                System.out.println("PercentBalance: " + balance);
//                System.out.println("curVal: " + curVal);
//                System.out.println("rebalanced: " + rebalanced);
//                System.out.println("netBalance: " + netBalance);
                if(netBalance > bestBalance) {
                    best = i;
                    bestBalance = netBalance;
                }
            }
            partVals[best] = curVal;
            return partVals;
        }
    }





}
