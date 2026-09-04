package com.finance.manager.repository;

import com.finance.manager.entity.Category;
import com.finance.manager.entity.Transaction;
import com.finance.manager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    List<Transaction> findByUserOrderByDateDescIdDesc(User user);
    boolean existsByCategory(Category category);
    List<Transaction> findByUserAndDateGreaterThanEqual(User user, LocalDate startDate);
    List<Transaction> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
}
