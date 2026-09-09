CREATE DATABASE yekdb_v1_demo;
USE DATABASE yekdb_v1_demo;

CREATE TABLE departments (
    id INT PRIMARY KEY,
    name STRING UNIQUE NOT NULL
);

CREATE TABLE employees (
    id INT PRIMARY KEY,
    department_id INT,
    name STRING NOT NULL,
    age INT NOT NULL,
    salary DECIMAL(10,2),
    FOREIGN KEY (department_id) REFERENCES departments(id)
);

CREATE TABLE employee_audit (
    id INT,
    employee_name STRING,
    action STRING
);

INSERT INTO departments (id, name) VALUES (1, 'Engineering');
INSERT INTO departments (id, name) VALUES (2, 'Research');

INSERT INTO employees (id, department_id, name, age, salary)
VALUES (101, 1, 'Ada', 28, 95000.00);
INSERT INTO employees (id, department_id, name, age, salary)
VALUES (102, 1, 'Grace', 32, 105000.00);
INSERT INTO employees (id, department_id, name, age, salary)
VALUES (103, 2, 'Alan', 24, 88000.00);

SELECT id, name, age, salary
FROM employees
WHERE age >= 25
ORDER BY salary DESC;

SELECT employees.name, departments.name
FROM employees
INNER JOIN departments
ON employees.department_id = departments.id;

CREATE INDEX idx_employees_age ON employees(age);

EXPLAIN ANALYZE SELECT id, name
FROM employees
WHERE age >= 25;

CREATE VIEW senior_employees AS
SELECT id, name, age
FROM employees
WHERE age >= 30;

SELECT * FROM senior_employees;

CREATE TRIGGER employees_after_insert_audit
AFTER INSERT ON employees
BEGIN
    INSERT INTO employee_audit (id, employee_name, action)
    VALUES (NEW.id, NEW.name, 'INSERT')
END;

BEGIN;
INSERT INTO employees (id, department_id, name, age, salary)
VALUES (104, 2, 'Edsger', 35, 110000.00);
SAVEPOINT before_raise;
UPDATE employees SET salary = 115000.00 WHERE id = 104;
ROLLBACK TO SAVEPOINT before_raise;
COMMIT;

SELECT * FROM senior_employees;
SELECT * FROM employee_audit;
SHOW TRANSACTION;
SHOW VIEWS;
SHOW TRIGGERS;

\q
