package org.halocambodia.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CareerTypeRepository  extends JpaRepository<CareerType, Long>, JpaSpecificationExecutor<CareerType> {

}
