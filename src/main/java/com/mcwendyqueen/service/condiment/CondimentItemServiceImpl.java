package com.mcwendyqueen.service.condiment;

import java.util.List;
import java.util.Optional;

import lombok.Getter;

import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;
import com.mcwendyqueen.model.condiment.CondimentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.CHEESE;
import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.KETCHUP;
import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.LETTUCE;
import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.MUSTARD;
import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.ONIONS;
import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.PICKLES;
import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.TOMATOES;

@Service
public class CondimentItemServiceImpl implements CondimentItemService {

    public static final long UNKNOWN_CONDIMENT_ITEM = -1;

    private final CondimentRepository condimentRepository;

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
            return this.shortName;
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
        Optional<CondimentItem> existingCondiment =
                condimentRepository.findByName(newCondiment.getName());

        if (existingCondiment.isPresent()) {
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

        if (condimentToDelete.isPresent()) {
            condimentRepository.deleteById(condimentId);
            deletedCondiment = condimentToDelete.get();
        }

        return deletedCondiment;
    }

    @Override
    public Optional<CondimentItem> deleteCondimentItem(long condimentId) {
        Optional<CondimentItem> condimentToDelete = condimentRepository.findById(condimentId);

        if (condimentToDelete.isPresent()) {
            condimentRepository.deleteById(condimentId);
        }

        return condimentToDelete;
    }

    @Override
    public Optional<CondimentItem> deleteCondimentItem(String condimentName) {
        Optional<CondimentItem> condimentToDelete = condimentRepository.findByName(condimentName);

        if (condimentToDelete.isPresent()) {
            condimentRepository.deleteById(condimentToDelete.get().getId());
        }

        return condimentToDelete;
    }

    @Override
    public List<CondimentItem> findAllCondimentItemsForMenuItem(Long menuId) {
        return condimentRepository.findAllCondimentItemsForMenuItem(menuId);
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

        if (existingCondiment.isPresent()) {
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
