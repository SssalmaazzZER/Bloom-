package ma.bloome.repository;

import ma.bloome.entity.Enums.Role;
import ma.bloome.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    @Query("""
            select u from User u
            where u.role = :role
              and (:q = '' or lower(u.email) like concat('%', :q, '%')
                   or lower(concat(u.firstName, ' ', u.lastName)) like concat('%', :q, '%'))
            """)
    Page<User> search(@Param("role") Role role, @Param("q") String q, Pageable pageable);
}
