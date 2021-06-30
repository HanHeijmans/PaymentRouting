package paymentrouting.route.split;

import java.util.*;

import gtna.graph.Graph;
import gtna.graph.Node;
import org.apache.commons.math3.util.CombinatoricsUtils;
import paymentrouting.route.ClosestNeighbor;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;

public class SplitCapacityDistance extends PathSelection {
    ClosestNeighbor cn;

    public SplitCapacityDistance(DistanceFunction df) {
        super("SPLIT_CAPACITY_DISTANCE", df);
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
//        System.out.println("OutNodes: " + Arrays.toString(out));
//        System.out.println();
//        for(int i : out) {
//            System.out.println(this.dist.isCloser(i, cur, dst, reality) + " Dist: " + this.dist.distance(i, dst, reality));
//        }
        //sum all funds that can be forwarded
        double sum = 0;
        HashMap<Double, Vector<Integer>> pots = new HashMap<Double, Vector<Integer>>();
        HashMap<Integer, Double> dists = new HashMap<>();
        HashMap<Integer, Double> potss = new HashMap<>();
        for (int k = 0; k < out.length; k++) {
            if (out[k] == pre || excluded[out[k]]) continue;
            if (this.dist.isCloser(out[k], cur, dst, reality)) {
//                System.out.println("Considering: " + k);
                double pot = rp.computePotential(cur, out[k]);
                Vector<Integer> vecPot = pots.get(pot);
                if (vecPot == null) {
                    vecPot = new Vector<Integer>();
                    pots.put(pot, vecPot);
                }
                vecPot.add(k);

                double d = this.dist.distance(out[k], dst, reality);
                potss.put(k, pot);
                dists.put(k, d);

                sum = sum + pot;
            }
        }
//        System.out.println("Sum: " + sum);
        if (sum < curVal) {
            //combined balances insufficient -> routing fails
            return null;
        } else {
            //sort nodes by potential (available funds)
            double[] partVal = new double[out.length];

            Iterator<Double> potIt = pots.keySet().iterator();
            double[] potVals = new double[pots.size()];
            int c = 0;
            while (potIt.hasNext()) {
                potVals[c] = potIt.next();
                c++;
            }
            Arrays.sort(potVals);

            Iterator<Integer> it = dists.keySet().iterator();
            int[] mapping = new int[dists.size()];
            c = 0;
            while(it.hasNext()) {
                mapping[c] = it.next();
                c++;
            }

            //find min splittage
            int splits = 0;
            double all = 0;
            for(int i = potVals.length - 1; i > -1; i--) {
                Vector<Integer> vec = pots.get(potVals[i]);
                for(int j = 0; j < vec.size(); j++) {
                    splits++;
                    all += potVals[i];
                    if(all >= curVal) {
                        break;
                    }
                }
                if(all >= curVal) {
                    break;
                }
            }

            //Determining min split minimal dist combinations
            Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(dists.size(), splits);
            LinkedList<int[]> minList = new LinkedList<>();
            double minimalDist = Double.MAX_VALUE;
            while(iterator.hasNext()) {
                int[] comb = iterator.next();
                double totalPot = 0;
                double totalDist = 0;
                for(int i : comb) {
                    totalPot += potss.get(mapping[i]);
                    totalDist += dists.get(mapping[i]);
                }
//                if(totalDist == minimalDist && totalPot > curVal) {
//                    minList.add(comb);
//                }
                if(totalPot > curVal && totalDist < minimalDist) {
                    minList = new LinkedList<>();
                    minList.add(comb);
                    minimalDist = totalDist;
                }
//                System.out.println(Arrays.toString(comb));
//                System.out.println("Dist: " + totalDist + " Pot: " + totalPot);
            }

            //splitting ties
            int[] best = minList.get(0);
//            double maxLookaheadBalance = 0;
//            if(minList.size() > 1) {
////                System.out.println("Tie found");
//                for(int[] comb : minList) {
//                    double totalLookaheadBalance = 0;
//                    for(int node : comb) {
//                        totalLookaheadBalance += this.computeTotalPotential(g, out[mapping[node]], dst, cur, excluded, rp, curVal, rand, reality);
//                    }
//                    if(totalLookaheadBalance > maxLookaheadBalance) {
//                        best = comb;
//                    }
//                }
//            }

            //allocation
            all = 0;
            double balance = 0;
            for(int i : best) {
                if(all > curVal) break;
                balance += potss.get(mapping[i]);
            }
            double saturation = curVal/balance;
            for(int i : best) {
                double value = Math.min(curVal - all, saturation * potss.get(mapping[i]));
                partVal[i] = value;
                all += value;
            }
//            System.out.println(all + " " + curVal);
//            System.out.println(Arrays.toString(partVal));
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
