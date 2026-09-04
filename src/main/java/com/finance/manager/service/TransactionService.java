package com.finance.manager.service;

import com.finance.manager.dto.CreateTransactionRequest;
import com.finance.manager.dto.MessageResponse;
import com.finance.manager.dto.TransactionListResponse;
import com.finance.manager.dto.TransactionResponse;
import com.finance.manager.dto.UpdateTransactionRequest;
import com.finance.manager.entity.Category;
import com.finance.manager.entity.CategoryType;
import com.finance.manager.entity.Transaction;
import com.finance.manager.entity.User;
import com.finance.manager.exception.BadRequestException;
import com.finance.manager.exception.ForbiddenException;
import com.finance.manager.exception.ResourceNotFoundException;
import com.finance.manager.repository.TransactionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final AuthService authService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryService categoryService,
                              AuthService authService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        User user = authService.getCurrentUser();

        if (request.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Transaction date cannot be in the future");
        }

        Category category = categoryService.getValidCategory(request.getCategory(), user);

        Transaction transaction = new Transaction(
                request.getAmount(),
                request.getDate(),
                category,
                request.getDescription(),
                category.getType(),
                user
        );

        Transaction saved = transactionRepository.save(transaction);
        return toResponse(saved);
    }

    public TransactionListResponse getTransactions(LocalDate startDate,
                                                   LocalDate endDate,
                                                   Long categoryId,
                                                   String categoryName,
                                                   CategoryType type) {
        User user = authService.getCurrentUser();

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user"), user));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (categoryName != null && !categoryName.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category").get("name")), categoryName.trim().toLowerCase()));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (query != null) {
                query.orderBy(cb.desc(root.get("date")), cb.desc(root.get("id")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Transaction> transactions = transactionRepository.findAll(spec);
        List<TransactionResponse> responses = transactions.stream()
                .map(this::toResponse)
                .toList();

        return new TransactionListResponse(responses);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, UpdateTransactionRequest request) {
        User user = authService.getCurrentUser();

        Optional<Transaction> optionalTransaction = transactionRepository.findById(id);
        if (optionalTransaction.isEmpty()) {
            throw new ResourceNotFoundException("Transaction not found: " + id);
        }

        Transaction transaction = optionalTransaction.get();
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot access another user's transaction");
        }

        if (request.getDate() != null && !request.getDate().equals(transaction.getDate())) {
            throw new BadRequestException("Transaction date cannot be modified");
        }

        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            Category category = categoryService.getValidCategory(request.getCategory(), user);
            transaction.setCategory(category);
        }

        Transaction updated = transactionRepository.save(transaction);
        return toResponse(updated);
    }

    @Transactional
    public MessageResponse deleteTransaction(Long id) {
        User user = authService.getCurrentUser();

        Optional<Transaction> optionalTransaction = transactionRepository.findById(id);
        if (optionalTransaction.isEmpty()) {
            throw new ResourceNotFoundException("Transaction not found: " + id);
        }

        Transaction transaction = optionalTransaction.get();
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot delete another user's transaction");
        }

        transactionRepository.delete(transaction);
        return new MessageResponse("Transaction deleted successfully");
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getDate(),
                transaction.getCategory().getName(),
                transaction.getDescription(),
                transaction.getType()
        );
    }
}
