package ma.bloome.repository;

import ma.bloome.entity.NewsletterSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterSubscriptionRepository extends JpaRepository<NewsletterSubscription, Long> {
    boolean existsByEmail(String email);
}
