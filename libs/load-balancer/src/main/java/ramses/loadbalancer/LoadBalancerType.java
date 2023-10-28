package ramses.loadbalancer;

import ramses.loadbalancer.algorithms.RandomLoadBalancer;
import ramses.loadbalancer.algorithms.WeightedRandomLoadBalancer;
import ramses.loadbalancer.algorithms.WeightedRoundRobinLoadBalancer;
import ramses.loadbalancer.algorithms.RoundRobinLoadBalancer;

public enum LoadBalancerType {
    ROUND_ROBIN,
    RANDOM,
    WEIGHTED_RANDOM,
    WEIGHTED_ROUND_ROBIN;
    //Custom

    public Class<? extends BaseLoadBalancer> getLoadBalancerClass() {
        //log.info("Test property: {}", common);
        return switch (this) {
            case ROUND_ROBIN -> RoundRobinLoadBalancer.class;
            case RANDOM -> RandomLoadBalancer.class;
            case WEIGHTED_RANDOM -> WeightedRandomLoadBalancer.class;
            case WEIGHTED_ROUND_ROBIN -> WeightedRoundRobinLoadBalancer.class;
        };
    }
}
