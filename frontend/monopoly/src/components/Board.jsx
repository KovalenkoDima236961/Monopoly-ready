import React from "react";
import Tile from "./Tile";
import "../css/Board.css";

const tiles = [
    { name: "Go", type: "corner go", backgroundImage: "/assets/goField.webp" },
    {
        name: "Chanel",
        type: "property",
        cost: 5000,
        backgroundImage: "/assets/Chanel_logo_interlocking_cs.png",
    },
    {
        name: "Question Mark",
        type: "chance",
        backgroundImage: "/assets/question-mark.jpg",
    },
    {
        name: "Boss",
        type: "property",
        cost: 5000,
        backgroundImage: "/assets/boss.png",
    },
    { name: "Money", type: "utility", backgroundImage: "/assets/money.png" },
    {
        name: "Mercedes",
        type: "cars",
        cost: 3000,
        backgroundImage: "/assets/Mercedes-Logo.png",
    },
    {
        name: "Adidas",
        type: "clothing",
        cost: 5000,
        backgroundImage: "/assets/Adidas_Logo.png",
    },
    {
        name: "Question Mark",
        type: "chance",
        backgroundImage: "/assets/question-mark.jpg",
    },
    {
        name: "Nike",
        type: "clothing",
        cost: 5000,
        backgroundImage: "/assets/Nike-Logo.png",
    },
    {
        name: "Lacoste",
        type: "clothing",
        cost: 5000,
        backgroundImage: "/assets/Lacoste-Logo.png",
    },
    {
        name: "Go to jail",
        type: "corner go-to-jail",
        backgroundImage: "/assets/jail.jpg",
    },
    {
        name: "Instagram",
        type: "social",
        cost: 5000,
        backgroundImage: "/assets/Instagram_logo_2016.png",
    },
    {
        name: "Rockstar",
        type: "games",
        cost: 5000,
        backgroundImage: "/assets/rocstar.jpg",
    },
    {
        name: "X",
        type: "social",
        cost: 5000,
        backgroundImage: "/assets/X-Logo.png",
    },
    {
        name: "Tik Tok",
        type: "social",
        cost: 5000,
        backgroundImage: "/assets/TikTok_logo.png",
    },
    {
        name: "Ferrari",
        type: "cars",
        cost: 5000,
        backgroundImage: "/assets/Ferrari-Emblem.png",
    },
    {
        name: "Coca Cola",
        type: "drinks",
        cost: 5000,
        backgroundImage: "/assets/Coca-Cola_logo.png",
    },
    {
        name: "Question Mark",
        type: "chance",
        backgroundImage: "/assets/question-mark.jpg",
    },
    {
        name: "Pepsi",
        type: "drinks",
        cost: 5000,
        backgroundImage: "/assets/Pepsi_2023.png",
    },
    {
        name: "Sprite",
        type: "drinks",
        cost: 5000,
        backgroundImage: "/assets/Sprite-Logo-2019.png",
    },
    {
        name: "Casino",
        type: "corner casino",
        backgroundImage: "/assets/casino.jpg",
    },
    {
        name: "Ryanair",
        type: "aircompany",
        cost: 5000,
        backgroundImage: "/assets/ryanair.png",
    },
    {
        name: "Question Mark",
        type: "chance",
        backgroundImage: "/assets/question-mark.jpg",
    },
    {
        name: "British Airways",
        type: "aircompany",
        cost: 5000,
        backgroundImage: "/assets/british-airways.webp",
    },
    {
        name: "Qatar Airways",
        type: "aircompany",
        cost: 5000,
        backgroundImage: "/assets/Qatar_Airways_logo.png",
    },
    {
        name: "Aston Martin",
        type: "cars",
        cost: 5000,
        backgroundImage: "/assets/aston_martin_logo.webp",
    },
    {
        name: "Burger King",
        type: "fastfood",
        cost: 5000,
        backgroundImage: "/assets/Burger_King_1999_logo.png",
    },
    {
        name: "McDonalds",
        type: "fastfood",
        cost: 5000,
        backgroundImage: "/assets/McDonalds-Logo.png",
    },
    {
        name: "Activision",
        type: "games",
        cost: 5000,
        backgroundImage: "/assets/Activision-logo.png",
    },
    {
        name: "KFC",
        type: "fastfood",
        cost: 5000,
        backgroundImage: "/assets/KFC_logo-image.png",
    },
    { name: "Prison", type: "corner" },
    { name: "Holiday Inn", type: "hotels", cost: 5000 },
    { name: "Radisson Blu", type: "hotels", cost: 5000 },
    { name: "Question Mark", type: "chance" },
    { name: "Novotel", type: "hotels", cost: 5000 },
    { name: "Porsche", type: "cars", cost: 5000 },
    { name: "Diamond", type: "utility" },
    { name: "Apple", type: "technology", cost: 5000 },
    { name: "Question Mark", type: "chance" },
    { name: "Nvidia", type: "technology", cost: 5000 },
    // Add more tiles as necessary
];

const Board = () => {
    const topRow = tiles.slice(0, 10);
    const rightColumn = tiles.slice(10, 20);
    const bottomRow = tiles.slice(20, 30).reverse();
    const leftRow = tiles.slice(30, 40).reverse();

    return (
        <div className="board-container">
            <div className="board">
                <div className="top-row">
                    {topRow.map((tile, index) => (
                        <div key={index} className="tile-wrapper">
                            {[
                                "property",
                                "cars",
                                "clothing",
                                "drinks",
                            ].includes(tile.type) && (
                                <div className={`tile-price ${tile.type}`}>
                                    {tile.cost && `${tile.cost}`}
                                </div>
                            )}
                            <Tile tile={tile} />
                        </div>
                    ))}
                </div>
                <div className="right-column">
                    {rightColumn.map((tile, index) => (
                        <div key={index} className="tile-wrapper">
                            {["social", "games", "drinks", "cars"].includes(
                                tile.type
                            ) && (
                                <div className={`tile-price-r ${tile.type}`}>
                                    ${tile.cost}
                                </div>
                            )}
                            <Tile
                                tile={tile}
                                className={
                                    tile.name !== "Go to jail"
                                        ? "right-column-tile"
                                        : ""
                                }
                            />
                        </div>
                    ))}
                </div>
                <div className="bottom-row">
                    {bottomRow.map((tile, index) => (
                        <div key={index} className="tile-wrapper">
                            {[
                                "aircompany",
                                "cars",
                                "games",
                                "fastfood",
                            ].includes(tile.type) && (
                                <div className={`tile-price ${tile.type}`}>
                                    {tile.cost && `${tile.cost}`}
                                </div>
                            )}
                            <Tile tile={tile} />
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default Board;
