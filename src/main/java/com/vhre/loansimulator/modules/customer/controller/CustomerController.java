package com.vhre.loansimulator.modules.customer.controller;

import com.vhre.base.core.base.controller.BaseController;
import com.vhre.loansimulator.modules.customer.dto.CustomerDTO;
import com.vhre.loansimulator.modules.customer.entity.Customer;
import com.vhre.loansimulator.modules.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "Endpoints for managing Customer")
public class CustomerController extends BaseController<Customer, CustomerDTO, UUID> {
    public CustomerController(CustomerService service) {
        super(service);
    }
}
