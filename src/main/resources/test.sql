use final_security;

CREATE TABLE users(
                      id INTEGER PRIMARY KEY,
                      username TEXT NOT NULL,
                      password TEXT NOT NULL
);

INSERT INTO users (id,username, password) VALUES
                                              (1,'abc', 'aaa'),
                                              (2,'def', 'ddd');

SELECT * FROM users;