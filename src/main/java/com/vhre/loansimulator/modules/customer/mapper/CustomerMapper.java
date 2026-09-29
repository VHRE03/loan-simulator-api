package com.vhre.loansimulator.modules.customer.mapper;

import com.vhre.base.core.base.mapper.BaseMapper;
import com.vhre.loansimulator.modules.customer.dto.CustomerDTO;
import com.vhre.loansimulator.modules.customer.entity.Customer;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface CustomerMapper extends BaseMapper<Customer, CustomerDTO> {
}
