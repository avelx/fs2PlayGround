CREATE TABLE StockPrice (
                        id SERIAL CONSTRAINT firstkey PRIMARY KEY,
                        company_name    varchar(40) NOT NULL,
                        trade_date      date,
                        price_open      float8
--                        code        char(5) CONSTRAINT firstkey PRIMARY KEY,
--                        title       varchar(40) NOT NULL,
--                        did         integer NOT NULL,
--                        date_prod   date,
--                        kind        varchar(10),
--                        len         interval hour to minute
);