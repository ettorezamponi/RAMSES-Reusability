package ramses.analyse.domain;

import ramses.knowledge.domain.adaptation.specifications.Availability;
import ramses.knowledge.domain.adaptation.specifications.AverageResponseTime;
import ramses.knowledge.domain.architecture.Instance;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class InstanceStats {
    private Instance instance;
    private double averageResponseTime;
    private double availability;
    private boolean fromNewData;

    public InstanceStats(Instance instance, double averageResponseTime, double availability) {
        this.instance = instance;
        this.averageResponseTime = averageResponseTime;
        this.availability = availability;
        fromNewData = true;
    }

    public InstanceStats(Instance instance) {
        this.instance = instance;
        availability = instance.getLatestValueForQoS(Availability.class).getDoubleValue();
        averageResponseTime = instance.getLatestValueForQoS(AverageResponseTime.class).getDoubleValue();
        this.fromNewData = false;
    }

}
