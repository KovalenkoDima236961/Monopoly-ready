import React from "react";
import "../css/Home.css";
import { FaDiscord, FaTelegramPlane, FaInstagram } from "react-icons/fa";

const Home = () => {
    return (
        <div className="home-container">
            {/* Hero Section */}
            <section className="hero-section">
                <div className="hero-content">
                    <h1>Приглашаем в Монополию.</h1>
                    <p>
                        Это отличное место, чтобы поиграть с друзьями в
                        легендарную настольную игру.
                    </p>
                    <button className="play-button">Начать игру</button>
                </div>
                <div className="hero-image-container">
                    <img
                        className="hero-image"
                        src="/assets/Monopoly.png"
                        alt="Game Screenshot"
                    />
                </div>
            </section>

            {/* Why Play Section */}
            <section className="why-play-section">
                <h2>Почему вам понравится играть у нас?</h2>
                <div className="reasons-container">
                    <div className="reason">
                        <img src="/assets/okay.svg" alt="Free" />
                        <h3>Это бесплатно!</h3>
                        <p>
                            Просто создайте аккаунт и начните играть — никаких
                            подписок и платежей.
                        </p>
                    </div>
                    <div className="reason">
                        <img src="/assets/dices.svg" alt="Modes" />
                        <h3>Множество режимов</h3>
                        <p>
                            Мы создали множество режимов с уникальными
                            механиками, в которых вы всегда испытаете что-то
                            новое. А классические правила были улучшены, чтобы
                            было интереснее играть в онлайне.
                        </p>
                    </div>
                    <div className="reason">
                        <img src="/assets/cup.svg" alt="Competitions" />
                        <h3>Соревнования</h3>
                        <p>
                            Играйте в Соревновательном режиме, чтобы получить
                            звание и поднимать его с каждой победой.
                        </p>
                    </div>
                    <div className="reason">
                        <img src="/assets/friends.svg" alt="Friends" />
                        <h3>Новые друзья</h3>
                        <p>
                            Находите интересных вам игроков и добавляйте их в
                            друзья. Общайтесь в личных сообщениях, обсуждая
                            новые тактики — или делитесь новыми мемами.
                        </p>
                    </div>
                    <div className="reason">
                        <img src="/assets/collection.svg" alt="Collection" />
                        <h3>Коллекционирование</h3>
                        <p>
                            У нас имеется множество предметов, некоторые из
                            которых очень редкие. Соберите себе коллекцию из
                            того, что вам больше нравится!
                        </p>
                    </div>
                    <div className="reason">
                        <img src="/assets/globe.svg" alt="Play Anywhere" />
                        <h3>Играйте где угодно</h3>
                        <p>
                            Вы можете играть на любом устройстве, где есть
                            браузер — хоть на компьютере из дома, хоть на
                            телефоне, пока едете в метро.
                        </p>
                    </div>
                </div>
            </section>

            {/* Footer Section */}
            <footer className="footer-section">
                <button className="play-button-footer">Начать игру</button>
                <div className="footer-content">
                    <div className="footer-left">
                        <img
                            src="/assets/logo-footer.png"
                            alt="Monopoly Logo"
                            className="footer-logo"
                        />
                        <p>Монополия — бесплатная онлайн-игра.</p>
                        <p>
                            Все бренды и торговые марки на этой странице
                            принадлежат правообладателям и размещены на правах
                            рекламы.
                        </p>
                    </div>
                    <div className="footer-center">
                        <h4>Материалы</h4>
                        <ul>
                            <li>
                                <a href="#rules">Правила Сайта</a>
                            </li>
                            <li>
                                <a href="#how-to-play">Как тут играть</a>
                            </li>
                            <li>
                                <a href="#complaints">О нарушениях</a>
                            </li>
                            <li>
                                <a href="#inventory">Об инвентаре</a>
                            </li>
                            <li>
                                <a href="#ranks">Ранги</a>
                            </li>
                            <li>
                                <a href="#docs">Документация</a>
                            </li>
                            <li>
                                <a href="#status">Статус Сайта</a>
                            </li>
                        </ul>
                    </div>
                    <div className="footer-right">
                        <h4>Соцсети</h4>
                        <ul className="social-icons">
                            <li>
                                <a href="#discord" aria-label="Discord">
                                    <FaDiscord size={30} color="#5865F2" />
                                </a>
                            </li>
                            <li>
                                <a href="#telegram" aria-label="Telegram">
                                    <FaTelegramPlane
                                        size={30}
                                        color="#0088CC"
                                    />
                                </a>
                            </li>
                            <li>
                                <a href="#instagram" aria-label="Instagram">
                                    <FaInstagram size={30} color="#E1306C" />
                                </a>
                            </li>
                        </ul>
                        <p>
                            Подписывайтесь на Монополию в соцсетях, чтобы быть в
                            курсе обновлений игры.
                        </p>
                    </div>
                </div>
            </footer>
        </div>
    );
};

export default Home;
