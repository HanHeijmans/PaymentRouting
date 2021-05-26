package paymentrouting.route;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.parameter.Parameter;

import java.util.HashMap;

public class RoutePaymentHan extends Metric {
    public RoutePaymentHan() {
        super("ROUT_PAYMENT_HAN", new Parameter[]{});
    }

    @Override
    public void computeData(Graph g, Network n, HashMap<String, Metric> m) {

    }

    @Override
    public boolean writeData(String folder) {
        return false;
    }

    @Override
    public Single[] getSingles() {
        return new Single[0];
    }

    @Override
    public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
        return false;
    }
}
