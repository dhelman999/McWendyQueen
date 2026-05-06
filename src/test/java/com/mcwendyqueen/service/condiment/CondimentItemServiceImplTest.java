package com.mcwendyqueen.service.condiment;

import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;
import com.mcwendyqueen.model.condiment.CondimentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CondimentItemServiceImplTest {
    private CondimentRepository condimentRepository;
    private CondimentItemServiceImpl condimentItemService;

    @BeforeEach
    void setUp() {
        condimentRepository = Mockito.mock(CondimentRepository.class);

        when(condimentRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(condimentRepository.save(any(CondimentItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        condimentItemService = new CondimentItemServiceImpl(condimentRepository);
        clearInvocations(condimentRepository);
    }

    @Test
    void getAllCondiments_returnsAll() {
        List<CondimentItem> condiments = List.of(new CondimentItem(1L, "ketchup"));
        when(condimentRepository.findAll()).thenReturn(condiments);

        List<CondimentItem> result = condimentItemService.getAllCondiments();

        assertEquals(1, result.size());
        assertEquals("ketchup", result.get(0).getName());
    }

    @Test
    void createCondimentItem_happyPath_savesAndReturns() {
        CondimentItemRequestDTO request = new CondimentItemRequestDTO("sriracha");
        when(condimentRepository.findByName("sriracha")).thenReturn(Optional.empty());
        when(condimentRepository.save(any(CondimentItem.class))).thenAnswer(invocation -> {
            CondimentItem condiment = invocation.getArgument(0);
            condiment.setId(42L);
            return condiment;
        });

        CondimentItem created = condimentItemService.createCondimentItem(request);

        assertEquals(42L, created.getId());
        assertEquals("sriracha", created.getName());
        verify(condimentRepository, times(1)).save(any(CondimentItem.class));
    }

    @Test
    void deleteCondimentItemById_happyPath_deletesAndReturns() {
        CondimentItem existing = new CondimentItem(5L, "mustard");
        when(condimentRepository.findById(5L)).thenReturn(Optional.of(existing));

        Optional<CondimentItem> deleted = condimentItemService.deleteCondimentItem(5L);

        assertTrue(deleted.isPresent());
        assertEquals("mustard", deleted.get().getName());
        verify(condimentRepository, times(1)).deleteById(5L);
    }
}
