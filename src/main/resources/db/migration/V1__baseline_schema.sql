-- Baseline: schema creato da Hibernate (ddl-auto=update) fino alla versione 1.0.2.
-- Sui database esistenti questa migrazione NON viene eseguita (baseline-version=1);
-- sui database vuoti crea lo schema identico a quello di produzione.

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `email_verified` bit(1) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  `username` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `refresh` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_at` datetime(6) NOT NULL,
  `expire_at` datetime(6) NOT NULL,
  `revoke_at` datetime(6) DEFAULT NULL,
  `token_hash` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKb7n5qn83vsw19kdj4xfctnhea` (`token_hash`),
  KEY `FK4gnf4aborh8m179simgynurme` (`user_id`),
  CONSTRAINT `FK4gnf4aborh8m179simgynurme` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `verification_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_at` datetime(6) NOT NULL,
  `expire_at` datetime(6) NOT NULL,
  `token_hash` varchar(255) NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKclh4tfurdi6ate1t2815l1vn8` (`token_hash`),
  KEY `FK54y8mqsnq1rtyf581sfmrbp4f` (`user_id`),
  CONSTRAINT `FK54y8mqsnq1rtyf581sfmrbp4f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `expenses` (
  `id_expenses` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(38,2) NOT NULL,
  `category` enum('bills','car','gifts','groceries','health','housing','installments','leisure','loan','other','transport') NOT NULL,
  `date` date NOT NULL,
  `description` varchar(30) DEFAULT NULL,
  `title` varchar(15) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id_expenses`),
  KEY `FKhpk0n2cbnfiuu5nrgl0ika3hq` (`user_id`),
  CONSTRAINT `FKhpk0n2cbnfiuu5nrgl0ika3hq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `incomes` (
  `id_incomes` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(38,2) NOT NULL,
  `category` enum('bonus','freelance','gift','investment','other','refund','rental','salary') NOT NULL,
  `date` date NOT NULL,
  `description` varchar(30) DEFAULT NULL,
  `title` varchar(15) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id_incomes`),
  KEY `FKfq6qeso6vbt9wu7dyhnx8tpu9` (`user_id`),
  CONSTRAINT `FKfq6qeso6vbt9wu7dyhnx8tpu9` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
