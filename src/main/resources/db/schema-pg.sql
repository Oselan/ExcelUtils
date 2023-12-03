DROP TABLE IF EXISTS "user";
CREATE TABLE IF NOT EXISTS "user" (
	id serial8 NOT NULL,
	first_name varchar NULL,
	last_name varchar NULL,
	CONSTRAINT user_un UNIQUE (first_name,last_name)
);
