package com.example.eventsphere.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GenericMapper {

    private final ModelMapper modelMapper;

    public GenericMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    /**
     * Maps a single object to another type.
     * Works for both Forward (Entity -> DTO) and Reverse (DTO -> Entity).
     */
    public <S, T> T map(S source, Class<T> targetClass) {
        return modelMapper.map(source, targetClass);
    }

    /**
     * Maps a list of objects to a list of another type.
     */
    public <S, T> List<T> mapList(List<S> sourceList, Class<T> targetClass) {
        return sourceList.stream()
                .map(element -> modelMapper.map(element, targetClass))
                .collect(Collectors.toList());
    }
}

