package com.vhre.loansimulator.pattern;

import com.vhre.base.core.base.dto.BaseDTO;
import com.vhre.base.core.base.entity.BaseEntity;
import com.vhre.base.core.base.mapper.BaseMapper;
import com.vhre.base.core.exceptions.ResourceNotFoundException;
import com.vhre.base.core.base.service.BaseServiceImpl;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * REFERENCE PATTERN: how to unit test the service layer of a module with
 * JUnit 5 + Mockito (no database and no Spring context required).
 *
 * <p>Every module in this project follows the internal starter architecture:</p>
 * <pre>
 * modules/<module>/entity/Foo        extends BaseEntity
 * modules/<module>/dto/FooDTO        extends BaseDTO
 * modules/<module>/mapper/FooMapper  extends BaseMapper<Foo, FooDTO>
 * modules/<module>/repository/FooRepository extends JpaRepository<Foo, UUID>
 * modules/<module>/service/FooService        (interface)
 * modules/<module>/service/FooServiceImpl    extends BaseServiceImpl<...>
 * </pre>
 *
 * <p>To test a ServiceImpl you do NOT need the database: mock the repository and
 * the mapper, inject them with {@code @InjectMocks} and stub the calls you
 * expect. Copy this class as {@code FooServiceTest} for each new module and
 * rename the sample classes accordingly. The nested classes below exist only to
 * make this pattern self-contained and compilable.</p>
 */
@ExtendWith(MockitoExtension.class)
class ServiceTestPatternTest {

    // -------------------------------------------------------------------------
    // Sample module classes (in a real module these are separate files)
    // -------------------------------------------------------------------------

    @Getter
    @Setter
    static class Sample extends BaseEntity {
        private String name;
    }

    @Getter
    @Setter
    static class SampleDTO extends BaseDTO {
        private String name;
    }

    interface SampleMapper extends BaseMapper<Sample, SampleDTO> {
    }

    interface SampleRepository extends JpaRepository<Sample, UUID> {
    }

    static class SampleServiceImpl extends BaseServiceImpl<Sample, SampleDTO, UUID> {
        SampleServiceImpl(SampleRepository repository, SampleMapper mapper) {
            super(repository, mapper);
        }
    }

    // -------------------------------------------------------------------------
    // Mocks: repository and mapper are replaced by Mockito stubs
    // -------------------------------------------------------------------------

    @Mock
    SampleRepository repository;

    @Mock
    SampleMapper mapper;

    @InjectMocks
    SampleServiceImpl service;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save converts the DTO to entity, persists it and returns the mapped DTO")
    void savePersistsEntityAndReturnsDto() {
        SampleDTO input = new SampleDTO();
        input.setName("BBVA");

        Sample entity = new Sample();
        entity.setName("BBVA");

        Sample saved = new Sample();
        saved.setName("BBVA");

        SampleDTO result = new SampleDTO();
        result.setName("BBVA");

        // given: stub the collaborator calls
        when(mapper.toEntity(input)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(result);

        // when
        SampleDTO actual = service.save(input);

        // then: assert the outcome and verify the interactions
        assertThat(actual).isNotNull();
        assertThat(actual.getName()).isEqualTo("BBVA");
        verify(mapper).toEntity(input);
        verify(repository).save(entity);
        verify(mapper).toDto(saved);
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the id does not exist")
    void findByIdThrowsWhenMissing() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository).findById(unknownId);
        verify(mapper, never()).toDto(any(Sample.class));
    }

    @Test
    @DisplayName("findById returns the mapped DTO when the id exists")
    void findByIdReturnsDtoWhenFound() {
        UUID id = UUID.randomUUID();
        Sample entity = new Sample();
        entity.setName("BBVA");

        SampleDTO dto = new SampleDTO();
        dto.setName("BBVA");

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        SampleDTO actual = service.findById(id);

        assertThat(actual.getName()).isEqualTo("BBVA");
        verify(mapper).toDto(entity);
    }
}
