package umc.exs.dtos;

import java.util.List;
import java.util.stream.Collectors;

public interface GenericMapper<E, D> {
    // Convert Entity to DTO
    D toDto(E entity);

    // Convert DTO to Entity
    E toEntity(D dto);

    // Handle lists automatically
    default List<D> toDtoList(List<E> entities) {
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    default List<E> toEntityList(List<D> dtos) {
        return dtos.stream().map(this::toEntity).collect(Collectors.toList());
    }
}