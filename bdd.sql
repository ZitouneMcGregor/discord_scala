USE AdminDiscord;

CREATE TABLE USER(
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE SERVER(
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    img VARCHAR(255)
    );

CREATE TABLE ROOM(
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    id_server INT NOT NULL,
    FOREIGN KEY (id_server) REFERENCES SERVER(id) ON DELETE CASCADE
);


CREATE TABLE PRIVATE_CHAT(
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id_1 INT NOT NULL,
    user_id_2 INT NOT NULL,
    FOREIGN KEY (user_id_1) REFERENCES USER(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id_2) REFERENCES USER(id) ON DELETE CASCADE,
    UNIQUE(user_id_1, user_id_2)
);
