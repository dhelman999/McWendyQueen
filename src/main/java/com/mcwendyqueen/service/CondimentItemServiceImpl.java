package com.mcwendyqueen.service;

import com.mcwendyqueen.model.CondimentItem;
import com.mcwendyqueen.model.CondimentItemRequestDTO;
import com.mcwendyqueen.model.CondimentRepository;
import com.mcwendyqueen.model.ModelMapperUtils;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.CHEESE;
import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.KETCHUP;
import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.LETTUCE;
import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.MUSTARD;
import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.ONIONS;
import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.PICKLES;
import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.TOMATOES;

@Service
public class CondimentItemServiceImpl implements  CondimentItemService {
    private final CondimentRepository condimentRepository;

    public static final long UNKNOWN_CONDIMENT_ITEM = -1;

    @Getter
    public enum CondimentEnum {
        LETTUCE("lettuce"),
        PICKLES("pickles"),
        TOMATOES("tomatoes"),
        CHEESE("cheese"),
        MUSTARD("mustard"),
        KETCHUP("ketchup"),
        ONIONS("onions");

        private final String shortName;

        CondimentEnum(String shortName) {
            this.shortName = shortName;
        }

        @Override
        public String toString() {
            return shortName;
        }
    }

    @Autowired
    public CondimentItemServiceImpl(CondimentRepository condimentRepository) {
        this.condimentRepository = condimentRepository;
        addAllCondiments();
    }

    @Override
    public List<CondimentItem> getAllCondiments() {
        return condimentRepository.findAll();
    }

    @Override
    public CondimentItem createCondimentItem(CondimentItemRequestDTO newCondiment) {
        Optional<CondimentItem> existingCondiment = condimentRepository.findByName(newCondiment.getName());

        if(existingCondiment.isPresent()) {
            // need to throw some problem or log
            return existingCondiment.get();
        }

        CondimentItem condimentToCreate = ModelMapperUtils.createCondimentItem(newCondiment);

        return condimentRepository.save(condimentToCreate);
    }

    @Override
    public CondimentItem deleteCondimentItem(CondimentItemRequestDTO condiment) {
        Long condimentId = getCondimentItemIdByName(condiment.getName());

        Optional<CondimentItem> condimentToDelete = condimentRepository.findById(condimentId);
        CondimentItem deletedCondiment = null;

        if(condimentToDelete.isPresent()) {
            condimentRepository.deleteById(condimentId);
            deletedCondiment = condimentToDelete.get();
        }

        return deletedCondiment;
    }

    @Override
    public long getCondimentItemIdByName(String name) {
        Optional<CondimentItem> condimentItems = condimentRepository.findByName(name);

        return condimentItems.map(CondimentItem::getId).orElse(UNKNOWN_CONDIMENT_ITEM);

    }

    @Override
    public Optional<CondimentItem> getCondimentByName(String condimentName) {
        return condimentRepository.findByName(condimentName);
    }

    @Override
    public Optional<CondimentItem> getCondimentItemById(long id) {
        return condimentRepository.findById(id);
    }

    public CondimentItem createCondimentItem(String name) {
        Optional<CondimentItem> existingCondiment = condimentRepository.findByName(name);

        if(existingCondiment.isPresent()) {
            // need to throw some problem or log
            return existingCondiment.get();
        }

        CondimentItem newCondiment = new CondimentItem(name);
        condimentRepository.save(newCondiment);

        return newCondiment;
    }

    private void addAllCondiments() {
        createCondimentItem(LETTUCE.getShortName());
        createCondimentItem(PICKLES.getShortName());
        createCondimentItem(TOMATOES.getShortName());
        createCondimentItem(CHEESE.getShortName());
        createCondimentItem(MUSTARD.getShortName());
        createCondimentItem(KETCHUP.getShortName());
        createCondimentItem(ONIONS.getShortName());
    }
}
