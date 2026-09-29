package com.vhre.loansimulator.modules.loan.mapper;

import com.vhre.base.core.base.mapper.BaseMapper;
import com.vhre.loansimulator.modules.loan.dto.LoanSimulationDTO;
import com.vhre.loansimulator.modules.loan.entity.LoanSimulation;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface LoanSimulationMapper extends BaseMapper<LoanSimulation, LoanSimulationDTO> {

    @Override
    @Mapping(target = "customer", ignore = true)
    LoanSimulation toEntity(LoanSimulationDTO dto);

    @Override
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromDto(LoanSimulationDTO dto, @MappingTarget LoanSimulation entity);

    @Override
    @Mapping(target = "customerId", source = "customer.id")
    LoanSimulationDTO toDto(LoanSimulation entity);
}
