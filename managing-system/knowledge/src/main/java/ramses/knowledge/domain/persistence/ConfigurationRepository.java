package ramses.knowledge.domain.persistence;

import ramses.knowledge.domain.architecture.ServiceConfiguration;
import org.springframework.data.repository.CrudRepository;

public interface ConfigurationRepository extends CrudRepository<ServiceConfiguration, Long> {

}
