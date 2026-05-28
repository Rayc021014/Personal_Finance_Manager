package com.skyfl.pfm.transaction.service;

import com.skyfl.pfm.account.entity.Account;
import com.skyfl.pfm.account.service.AccountService;
import com.skyfl.pfm.category.entity.Category;
import com.skyfl.pfm.category.service.CategoryService;
import com.skyfl.pfm.common.exception.BusinessException;
import com.skyfl.pfm.transaction.dto.TransactionRequest;
import com.skyfl.pfm.transaction.dto.TransactionResponse;
import com.skyfl.pfm.transaction.dto.TransactionUpdateRequest;
import com.skyfl.pfm.transaction.entity.Attachment;
import com.skyfl.pfm.transaction.entity.Transaction;
import com.skyfl.pfm.transaction.entity.TransactionType;
import com.skyfl.pfm.transaction.repository.AttachmentRepository;
import com.skyfl.pfm.transaction.repository.TransactionRepository;
import com.skyfl.pfm.user.entity.User;
import com.skyfl.pfm.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final AttachmentRepository attachmentRepository;
    private final StorageService storageService;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository,
            AccountService accountService, CategoryService categoryService,
            AttachmentRepository attachmentRepository, StorageService storageService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.attachmentRepository = attachmentRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> search(UUID userId, UUID accountId, UUID categoryId, TransactionType type,
            LocalDate startDate, LocalDate endDate, String keyword, Pageable pageable) {
        return transactionRepository.findAll(buildSearchSpecification(userId, accountId, categoryId, type, startDate, endDate,
                keyword), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public TransactionResponse create(UUID userId, TransactionRequest request) {
        if (request.type() == TransactionType.TRANSFER) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Use transfer endpoint for transfers");
        }
        Transaction transaction = new Transaction();
        transaction.setUser(getUser(userId));
        apply(userId, transaction, request.accountId(), request.categoryId(), request.amount(), request.transactionDate(),
                request.note());
        transaction.setType(request.type());
        transaction.setTransferPair(null);
        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID userId, UUID transactionId) {
        return toResponse(getEntity(userId, transactionId));
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID transactionId, TransactionUpdateRequest request) {
        Transaction transaction = getEntity(userId, transactionId);
        apply(userId, transaction, request.accountId(), request.categoryId(), request.amount(), request.transactionDate(),
                request.note());
        return toResponse(transaction);
    }

    @Transactional
    public void delete(UUID userId, UUID transactionId) {
        Transaction transaction = getEntity(userId, transactionId);
        transaction.setDeleted(true);
        if (transaction.getTransferPair() != null) {
            transaction.getTransferPair().setDeleted(true);
        }
    }

    @Transactional
    public TransactionResponse attachFile(UUID userId, UUID transactionId, MultipartFile file) {
        Transaction transaction = getEntity(userId, transactionId);
        StorageService.StoredFile storedFile = storageService.store(file);
        Attachment attachment = new Attachment();
        attachment.setTransaction(transaction);
        attachment.setFileName(storedFile.originalName());
        attachment.setFilePath(storedFile.absolutePath());
        attachment.setMimeType(storedFile.contentType() == null ? "application/octet-stream" : storedFile.contentType());
        attachment.setFileSize(storedFile.size());
        attachmentRepository.save(attachment);
        return toResponse(transaction);
    }

    public Transaction getEntity(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndUserIdAndDeletedFalse(transactionId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private void apply(UUID userId, Transaction transaction, UUID accountId, UUID categoryId,
            java.math.BigDecimal amount, java.time.LocalDate transactionDate, String note) {
        Account account = accountService.getEntity(userId, accountId);
        Category category = categoryService.getEntity(userId, categoryId);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setCurrency(account.getCurrency());
        transaction.setAmount(amount);
        transaction.setTransactionDate(transactionDate);
        transaction.setNote(note);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Specification<Transaction> buildSearchSpecification(UUID userId, UUID accountId, UUID categoryId,
            TransactionType type, LocalDate startDate, LocalDate endDate, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (accountId != null) {
                predicates.add(criteriaBuilder.equal(root.get("account").get("id"), accountId));
            }

            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("note"), "")),
                        "%" + keyword.toLowerCase(Locale.ROOT) + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getTransactionDate(),
                transaction.getNote(),
                transaction.getTransferPair() == null ? null : transaction.getTransferPair().getId()
        );
    }
}
