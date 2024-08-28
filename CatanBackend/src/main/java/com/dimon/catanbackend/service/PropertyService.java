package com.dimon.catanbackend.service;

import com.dimon.catanbackend.entities.Game;
import com.dimon.catanbackend.entities.Property;
import com.dimon.catanbackend.repositories.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    public void initializeProperties(Game game) {
        List<Property> properties = List.of(
                new Property(UUID.randomUUID().toString(), "Go", 0, 0, "corner", game, null, 0),
                new Property(UUID.randomUUID().toString(), "Chanel", 1, 5000, "pink", game, null, 100),
                new Property(UUID.randomUUID().toString(), "Question Mark", 2, 0, "utility", game, null, 0),
                new Property(UUID.randomUUID().toString(), "Boss", 3, 5000, "pink", game, null, 110),
                new Property(UUID.randomUUID().toString(), "Money", 4, 0, "utility", game, null, 0),
                new Property(UUID.randomUUID().toString(), "Mercedes", 5, 3000, "cars", game, null, 120),
                new Property(UUID.randomUUID().toString(), "Adidas", 6, 5000, "yellow", game, null, 130),
                new Property(UUID.randomUUID().toString(), "Question Mark", 7, 0, "utility", game, null, 0),
                new Property(UUID.randomUUID().toString(), "Nike", 8, 4000, "yellow", game, null, 140),
                new Property(UUID.randomUUID().toString(), "Lacoste", 9, 3000, "yellow", game, null, 150),
                new Property(UUID.randomUUID().toString(), "Go to jail", 10, 0, "corner", game, null, 0),
                new Property(UUID.randomUUID().toString(), "Instagram", 11, 3000, "social media", game, null, 160),
                new Property(UUID.randomUUID().toString(), "Rockstar", 12, 3000, "games", game, null, 170),
                new Property(UUID.randomUUID().toString(), "X", 13, 3000, "social media", game, null, 180),
                new Property(UUID.randomUUID().toString(), "Tik Tok", 14, 3000, "social media", game, null, 190),
                new Property(UUID.randomUUID().toString(), "Ferrari", 15, 2000, "cars", game, null, 200),
                new Property(UUID.randomUUID().toString(), "Coca Cola", 16, 3000, "drinks", game, null, 210),
                new Property(UUID.randomUUID().toString(), "Question Mark", 17, 0, "utility", game, null, 0),
                new Property(UUID.randomUUID().toString(), "Pepsi", 18, 3000, "drinks", game, null, 220),
                new Property(UUID.randomUUID().toString(), "Sprite", 19, 3000, "drinks", game, null, 230),
                new Property(UUID.randomUUID().toString(), "Casino", 20, 0, "corner", game, null, 0),
                new Property(UUID.randomUUID().toString(), "Ryanair", 21, 3400, "aircompany", game, null, 240),
                new Property(UUID.randomUUID().toString(), "Question Mark", 22, 0, "utility", game, null, 0),
                new Property(UUID.randomUUID().toString(), "British airways", 23, 3050, "aircompany", game, null, 250),
                new Property(UUID.randomUUID().toString(), "Qatar Airways", 24, 3500, "aircompany", game, null, 260),
                new Property(UUID.randomUUID().toString(), "Aston Martin", 25, 3000, "cars", game, null, 270),
                new Property(UUID.randomUUID().toString(), "Burger King", 26, 3000, "fastfood", game, null, 280),
                new Property(UUID.randomUUID().toString(), "McDonalds", 27, 3000, "fastfood", game, null, 290),
                new Property(UUID.randomUUID().toString(), "Activision", 28, 3000, "games", game, null, 300),
                new Property(UUID.randomUUID().toString(), "KFC", 29, 3000, "fastfood", game, null, 310),
                new Property(UUID.randomUUID().toString(), "Prison", 30, 0, "corner", game, null, 0),
                new Property(UUID.randomUUID().toString(), "HolidayInn", 31, 4000, "hotels", game, null, 320),
                new Property(UUID.randomUUID().toString(), "Radisson Blu", 32, 4000, "hotels", game, null, 330),
                new Property(UUID.randomUUID().toString(), "Question Mark", 33, 0, "utility", game, null, 0),
                new Property(UUID.randomUUID().toString(), "Novotel", 34, 4000, "hotels", game, null, 340),
                new Property(UUID.randomUUID().toString(), "Porsche", 35, 3000, "cars", game, null, 350),
                new Property(UUID.randomUUID().toString(), "Diamond", 36, 0, "utility", game, null, 0),
                new Property(UUID.randomUUID().toString(), "Apple", 37, 5000, "technology", game, null, 360),
                new Property(UUID.randomUUID().toString(), "Question Mark", 38, 0, "utility", game, null, 0),
                new Property(UUID.randomUUID().toString(), "Nvidia", 39, 5500, "technology", game, null, 370)
        );

        propertyRepository.saveAll(properties);
    }

    public Optional<Property> findByGameIdAndPropertyName(String gameId, String propertyName) {
        return propertyRepository.findByGameIdAndName(gameId, propertyName);
    }


    public void delete(Property property) {
        propertyRepository.delete(property);
    }

}
