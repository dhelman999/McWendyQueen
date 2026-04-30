package com.mcwendyqueen.service.condiment;

import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;

import java.util.List;
import java.util.Optional;

public interface CondimentItemService {
    List<CondimentItem> getAllCondiments();

    long getCondimentItemIdByName(String name);

    Optional<CondimentItem> getCondimentByName(String condimentName);

    Optional<CondimentItem> getCondimentItemById(long id);

    CondimentItem createCondimentItem(CondimentItemRequestDTO newCondiment);

    CondimentItem deleteCondimentItem(CondimentItemRequestDTO condiment);
}
