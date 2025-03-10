package com.secor.orderservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MainRestController.class);


    @Autowired
    MenuItemRepository menuItemRepository;

    public Integer calculateOrder(Order order)
    {

        log.info("Calculating total amount for order: {}", order);
        Integer total = 0;

        for(Map.Entry<String, Integer> entry : order.getDishes().entrySet())
        {

            log.info("Processing dish: {}", entry.getKey());
            MenuItem menuItem = menuItemRepository.findByDishid(entry.getKey());
            log.info("Found menu item: {}", menuItem);

            if(menuItem == null)
            {
                return -1;
            }

            total += menuItem.getPrice() * entry.getValue(); // multiplying price with quantity
            log.info("Total amount so far: {}", total);
        }

        log.info("Total amount calculated: {}", total);
        return total;
    }


}
