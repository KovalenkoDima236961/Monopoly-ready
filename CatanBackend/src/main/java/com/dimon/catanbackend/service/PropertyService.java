package com.dimon.catanbackend.service;

import com.dimon.catanbackend.entities.Game;
import com.dimon.catanbackend.entities.Property;
import com.dimon.catanbackend.repositories.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class responsible for handling business logic related to {@link Property} entities
 * in the context of the game. This class provides functionality for initializing properties
 * for a game, retrieving specific properties by game ID and property name, and deleting properties.
 *
 * The class interacts with the {@link PropertyRepository} for CRUD operations and manages
 * the property initialization for new games.
 *
 * Annotations used:
 * - {@link Service} to mark this as a Spring service component.
 * - {@link Autowired} to inject the necessary dependencies.
 *
 * Methods:
 * - {@code initializeProperties}: Initializes the predefined list of properties for a given game.
 * - {@code findByGameIdAndPropertyName}: Retrieves a property by the game ID and property name.
 * - {@code delete}: Deletes a specific property.
 *
 */
@Service
public class PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    /**
     * Initializes a list of predefined properties for the given {@link Game}. Each property
     * is associated with a unique identifier (UUID) and various attributes such as name, price,
     * and type. The properties are then saved in the repository.
     *
     * @param game the game for which the properties are being initialized
     */
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

    /**
     * Retrieves a property by the game ID and the property name.
     *
     * @param gameId the unique identifier of the game
     * @param propertyName the name of the property
     * @return an {@link Optional} containing the property if found, or empty otherwise
     */
    public Optional<Property> findByGameIdAndPropertyName(String gameId, String propertyName) {
        return propertyRepository.findByGameIdAndName(gameId, propertyName);
    }

    /**
     * Deletes a specific {@link Property} from the repository.
     *
     * @param property the property to be deleted
     */
    public void delete(Property property) {
        propertyRepository.delete(property);
    }

}
