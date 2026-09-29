package com.vhre.loansimulator.modules.loan.mapper;

import com.vhre.base.core.base.mapper.BaseMapper;
import com.vhre.loansimulator.modules.loan.dto.LoanSimulationDTO;
import com.vhre.loansimulator.modules.loan.entity.LoanSimulation;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface LoanSimulationMapper extends BaseMapper<LoanSimulation, LoanSimulationDTO> {
}
