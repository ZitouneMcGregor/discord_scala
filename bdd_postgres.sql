
\c "AdminDiscord"


CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT false
);

CREATE TABLE servers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    img VARCHAR(255)
);

CREATE TABLE room (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    id_server INT NOT NULL,
    FOREIGN KEY (id_server) REFERENCES servers(id) ON DELETE CASCADE
);

CREATE TABLE private_chat (
    id SERIAL PRIMARY KEY,
    user_id_1 INT NOT NULL,
    user_id_2 INT NOT NULL,
    delete_user_1 BOOLEAN DEFAULT false,
    delete_user_2 BOOLEAN DEFAULT false,
    FOREIGN KEY (user_id_1) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id_2) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id_1, user_id_2)
);

CREATE TABLE server_user (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    server_id INT NOT NULL,
    admin BOOLEAN DEFAULT false,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE,
    UNIQUE(user_id, server_id)
);

