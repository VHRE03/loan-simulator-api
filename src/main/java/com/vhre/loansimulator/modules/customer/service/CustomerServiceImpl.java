package com.vhre.loansimulator.modules.customer.service;

import com.vhre.base.core.base.service.BaseServiceImpl;
import com.vhre.loansimulator.modules.customer.dto.CustomerDTO;
import com.vhre.loansimulator.modules.customer.entity.Customer;
import com.vhre.loansimulator.modules.customer.mapper.CustomerMapper;
import com.vhre.loansimulator.modules.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomerServiceImpl extends BaseServiceImpl<Customer, CustomerDTO, UUID> implements CustomerService {
    public CustomerServiceImpl(CustomerRepository repository, CustomerMapper mapper) {
        super(repository, mapper);
    }
}
