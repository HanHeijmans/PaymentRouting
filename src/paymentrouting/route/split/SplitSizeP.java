package paymentrouting.route.split;

import gtna.graph.Graph;
import gtna.graph.Node;
import paymentrouting.route.ClosestNeighbor;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;

import java.util.*;

public class SplitSizeP extends PathSelection {
    double P;
    ClosestNeighbor cn;

    public SplitSizeP(DistanceFunction d, double P) {
        super("SPLIT_SIZE_P", d);
        cn = new ClosestNeighbor(d);
        this.P = P;
    }

    @Override
    public void initRoutingInfo(Graph g, Random rand) {
        this.dist.initRouteInfo(g, rand);
    }

    @Override
    public double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded, RoutePayment rp, double curVal, Random rand, int reality) {
        Node[] nodes = g.getNodes();
        int[] out = nodes[cur].getOutgoingEdges();

        double[] noSplit = this.cn.getNextsVals(g, cur, dst, pre, excluded, rp, curVal, rand, reality);
        if (noSplit != null) {
            return noSplit;
        }
        //Calculating the total potential
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
        //If total potential is smaller than transaction value payment fails
        if(sum < curVal) {
            return null;
        } else {
            //Update parameter in case transaction would otherwise fail
            double parameter = this.P;
            if(curVal / sum > this.P) {
                parameter = curVal / sum;
            }
//            System.out.println("Param " + parameter);
            //Sort in decreasing capacity size
            double[] partVal = new double[out.length];
            Iterator<Double> it = pots.keySet().iterator();
            double[] vals = new double[pots.size()];
            int c = 0;
            while (it.hasNext()) {
                vals[c] = it.next();
                c++;
            }
            Arrays.sort(vals);

            double all = 0;
            for (int i = vals.length - 1; i > -1; i--) {
                Vector<Integer> vec = pots.get(vals[i]);
                while (vec.size() > 0) {
                    int node = vec.remove(rand.nextInt(vec.size()));
                    double valNode = Math.min(rp.computePotential(cur, out[node]) * parameter, curVal-all);
//                    System.out.println(valNode);
                    all = all + valNode;
                    partVal[node] = valNode;
                    if (all >= curVal) {
                        break;
                    }
                }
                if (all >= curVal) {
                    break;
                }
            }
            return partVal;
        }
    }
}
