package ma.bloome.service;

import ma.bloome.entity.NewsletterSubscription;
import ma.bloome.repository.NewsletterSubscriptionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class NewsletterService {

    private final NewsletterSubscriptionRepository repository;

    public NewsletterService(NewsletterSubscriptionRepository repository) {
        this.repository = repository;
    }

    /**
     * Idempotent subscription. The response is identical whether or not the address was
     * already subscribed, so the endpoint cannot be used to discover subscribers.
     */
    // Deliberately not @Transactional: each repository call runs in its own transaction so a
    // concurrent duplicate insert can be absorbed without poisoning an outer transaction.
    public void subscribe(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmail(normalized)) {
            return;
        }
        try {
            NewsletterSubscription subscription = new NewsletterSubscription();
            subscription.setEmail(normalized);
            repository.saveAndFlush(subscription);
        } catch (DataIntegrityViolationException concurrentInsert) {
            // Another request subscribed the same address at the same time: same outcome.
        }
    }
}
