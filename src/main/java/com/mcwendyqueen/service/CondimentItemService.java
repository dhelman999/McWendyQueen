package com.mcwendyqueen.service;

import com.mcwendyqueen.model.CondimentItem;
import com.mcwendyqueen.model.CondimentItemRequestDTO;

import java.util.List;

public interface CondimentItemService {
    List<CondimentItem> getAllCondiments();

    CondimentItem createCondimentItem(CondimentItemRequestDTO newCondiment);

    CondimentItem deleteCondimentItem(CondimentItemRequestDTO condiment);
}
