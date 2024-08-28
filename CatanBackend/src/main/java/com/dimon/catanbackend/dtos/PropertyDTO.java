package com.dimon.catanbackend.dtos;

import com.dimon.catanbackend.entities.Property;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PropertyDTO {
    private String id;
    private String name;
    private int position;
    private int offices;
    private int cost;
    private String category;
    private int baseRent;

    public PropertyDTO(Property property) {
        this.id = property.getId();
        this.name = property.getName();
        this.position = property.getPosition();
        this.cost = property.getCost();
        this.offices = property.getOffices();
        this.category = property.getCategory();
        this.baseRent = property.getBaseRent();
    }
}
