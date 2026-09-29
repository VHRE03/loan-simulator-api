package com.vhre.loansimulator.modules.customer.service;

import com.vhre.base.core.exceptions.ResourceNotFoundException;
import com.vhre.loansimulator.modules.customer.dto.CustomerDTO;
import com.vhre.loansimulator.modules.customer.entity.Customer;
import com.vhre.loansimulator.modules.customer.mapper.CustomerMapper;
import com.vhre.loansimulator.modules.customer.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomerServiceImpl}, following the reference pattern
 * of {@code pattern.ServiceTestPatternTest}: pure JUnit 5 + Mockito, no
 * database and no Spring context.
 *
 * <p>The service is the plain starter CRUD (adult age and uniqueness rules
 * live in bean validation and in the database), so these tests document the
 * module's mapping flow with its own types.</p>
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    CustomerRepository repository;

    @Mock
    CustomerMapper mapper;

    @InjectMocks
    CustomerServiceImpl service;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Customer entityOf(CustomerDTO dto) {
        return Customer.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .nationalId(dto.getNationalId())
                .dateOfBirth(dto.getDateOfBirth())
                .build();
    }

    private CustomerDTO dtoOf(Customer entity) {
        CustomerDTO dto = new CustomerDTO();
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setNationalId(entity.getNationalId());
        dto.setDateOfBirth(entity.getDateOfBirth());
        return dto;
    }

    private CustomerDTO sampleInput() {
        CustomerDTO input = new CustomerDTO();
        input.setFirstName("Maria");
        input.setLastName("Gonzalez Lopez");
        input.setEmail("maria.gonzalez@example.com");
        input.setPhoneNumber("+52 55 1234 5678");
        input.setNationalId("GOLM900101MDFRNR09");
        input.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return input;
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save converts the DTO to entity, persists it and returns the mapped DTO")
    void savePersistsCustomerAndReturnsDto() {
        CustomerDTO input = sampleInput();
        Customer entity = entityOf(input);

        when(mapper.toEntity(input)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dtoOf(entity));

        CustomerDTO actual = service.save(input);

        assertThat(actual).isNotNull();
        assertThat(actual.getEmail()).isEqualTo("maria.gonzalez@example.com");
        assertThat(actual.getNationalId()).isEqualTo("GOLM900101MDFRNR09");
        verify(mapper).toEntity(input);
        verify(repository).save(entity);
        verify(mapper).toDto(entity);
    }

    @Test
    @DisplayName("findById returns the mapped DTO when the id exists")
    void findByIdReturnsDtoWhenFound() {
        UUID id = UUID.randomUUID();
        Customer entity = entityOf(sampleInput());

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dtoOf(entity));

        CustomerDTO actual = service.findById(id);

        assertThat(actual.getEmail()).isEqualTo("maria.gonzalez@example.com");
        verify(mapper).toDto(entity);
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the id does not exist")
    void findByIdThrowsWhenMissing() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository).findById(unknownId);
        verify(mapper, never()).toDto(any(Customer.class));
    }
}
