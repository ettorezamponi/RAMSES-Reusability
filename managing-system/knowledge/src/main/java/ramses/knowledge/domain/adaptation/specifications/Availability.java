package ramses.knowledge.domain.adaptation.specifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Getter
@Setter
@Slf4j
public class Availability extends QoSSpecification {
    @JsonProperty("min_threshold")
    private double minThreshold;


    @JsonCreator
    public Availability() { super(); }

    // used in QoSParser: clazz.getDeclaredConstructor(String.class)
    public Availability(String json) {
        super();
        fromJson(json);
    }

    @Override
    void fromJson(String json) {
        Gson gson = new Gson();
        JsonObject parameter = gson.fromJson(json, JsonObject.class).getAsJsonObject();
        setWeight(parameter.get("weight").getAsDouble());
        minThreshold = parameter.get("min_threshold").getAsDouble();
    }

    @Override
    public String getConstraintDescription() {
        return "value > "+ String.format(Locale.ROOT,"%.2f", minThreshold*100) + "%";
    }

    @Override
    @JsonIgnore
    public boolean isSatisfied(double value) {
        return value >= minThreshold;
    }

}
