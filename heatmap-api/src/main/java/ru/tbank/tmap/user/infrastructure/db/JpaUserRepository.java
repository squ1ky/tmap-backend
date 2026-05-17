package ru.tbank.tmap.user.infrastructure.db;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.user.domain.UserRepository;
import ru.tbank.tmap.user.domain.UserSearchCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface JpaUserRepository
        extends UserRepository, JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    @Override
    default Page<User> search(final UserSearchCriteria criteria, final Pageable pageable) {
        return findAll(toSpecification(criteria), pageable);
    }

    private static Specification<User> toSpecification(final UserSearchCriteria criteria) {
        return (root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (criteria.nickname() != null && !criteria.nickname().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("nickname")),
                        "%" + criteria.nickname().toLowerCase() + "%"
                ));
            }
            if (criteria.email() != null && !criteria.email().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("email")),
                        "%" + criteria.email().toLowerCase() + "%"
                ));
            }
            if (criteria.role() != null) {
                predicates.add(cb.equal(root.get("role"), criteria.role()));
            }
            if (criteria.blocked() != null) {
                predicates.add(cb.equal(root.get("blocked"), criteria.blocked()));
            }
            if (criteria.createdFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.createdFrom()));
            }
            if (criteria.createdTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.createdTo()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
