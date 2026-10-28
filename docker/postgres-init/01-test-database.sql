-- Creates the database used by the backend integration tests (mvn test) on first container start.
-- The tests clean and re-migrate this database on every run; never point them at real data.
CREATE DATABASE bloome_test;
