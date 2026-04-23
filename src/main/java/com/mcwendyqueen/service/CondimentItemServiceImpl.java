package com.mcwendyqueen.service;

import com.mcwendyqueen.model.CondimentItem;
import com.mcwendyqueen.model.CondimentItemRequestDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CondimentItemServiceImpl implements  CondimentItemService {
    private final Map<Long, CondimentItem> condimentsMap;

    private long condimentId;

    public CondimentItemServiceImpl() {
        condimentsMap = new ConcurrentHashMap<>();
        condimentId = 1;
        addAllCondiments();
    }

    @Override
    public List<CondimentItem> getAllCondiments() {
        return new ArrayList<>(condimentsMap.values());
    }

    @Override
    public CondimentItem createCondimentItem(CondimentItemRequestDTO newCondiment) {
        CondimentItem createdCondiment = new CondimentItem();
        createdCondiment.setId(condimentId++);
        createdCondiment.setName(newCondiment.getName());
        condimentsMap.put(condimentId, createdCondiment);

        return createdCondiment;
    }

    @Override
    public CondimentItem deleteCondimentItem(CondimentItemRequestDTO condiment) {
        Optional<CondimentItem> removedCondiment = condimentsMap.values().stream()
                .filter(item -> item.getName().equals(condiment.getName()))
                        .findFirst();

        CondimentItem removedItem = removedCondiment.orElse(null);

        if(removedItem != null) {
            condimentsMap.remove(condimentId);
        }

        return removedItem;
    }

    private void addAllCondiments() {
        CondimentItem newCondiment = new CondimentItem(condimentId++, "lettuce");
        condimentsMap.put(newCondiment.getId(), newCondiment);

        newCondiment = new CondimentItem(condimentId++,"pickles");
        condimentsMap.put(newCondiment.getId(), newCondiment);

        newCondiment = new CondimentItem(condimentId++,"tomatoes");
        condimentsMap.put(newCondiment.getId(), newCondiment);

        newCondiment = new CondimentItem(condimentId++,"cheese");
        condimentsMap.put(newCondiment.getId(), newCondiment);

        newCondiment = new CondimentItem(condimentId++,"mustard");
        condimentsMap.put(newCondiment.getId(), newCondiment);

        newCondiment = new CondimentItem(condimentId++,"ketchup");
        condimentsMap.put(newCondiment.getId(), newCondiment);
    }
}
