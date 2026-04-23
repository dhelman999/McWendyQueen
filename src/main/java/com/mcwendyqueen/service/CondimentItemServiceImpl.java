package com.mcwendyqueen.service;

import com.mcwendyqueen.model.CondimentItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CondimentItemServiceImpl implements  CondimentItemService {
    @Override
    public List<CondimentItem> GetAllCondiments() {
        return new ArrayList<>();
    }
}
