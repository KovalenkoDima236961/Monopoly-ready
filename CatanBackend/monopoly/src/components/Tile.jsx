import React from "react";
import "../css/Tile.css";

const Tile = ({ tile, className }) => {
    const rotationClass =
        tile.type === "property" ||
        tile.type === "clothing" ||
        tile.type === "fastfood" ||
        tile.type === "aircompany" ||
        tile.type === ""
            ? "rotate-270"
            : "";
    const tileClass = `tile ${className ? className : ""}`;

    return (
        <div className={`tile ${tile.type} ${tileClass}`}>
            <div
                className={`tile-image ${rotationClass}`}
                style={{ backgroundImage: `url(${tile.backgroundImage})` }}
            />
        </div>
    );
};

export default Tile;
