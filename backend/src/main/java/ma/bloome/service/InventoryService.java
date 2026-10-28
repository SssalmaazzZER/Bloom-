package ma.bloome.service;

import ma.bloome.entity.BouquetSnapshot;
import ma.bloome.entity.CustomerOrder;
import ma.bloome.entity.OrderItem;
import ma.bloome.exception.InsufficientStockException;
import ma.bloome.repository.BouquetStemRepository;
import ma.bloome.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

/**
 * Stock reservation and release. Every decrement is a conditional, atomic UPDATE
 * ({@code stock >= qty}); rows are always touched in ascending id order so concurrent
 * checkouts cannot deadlock. Must run inside the caller's transaction so that a failure
 * on any line rolls back every previous decrement.
 */
@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final BouquetStemRepository stemRepository;

    public InventoryService(ProductRepository productRepository, BouquetStemRepository stemRepository) {
        this.productRepository = productRepository;
        this.stemRepository = stemRepository;
    }

    /**
     * @param productQuantities product id → quantity
     * @param stemQuantities    stem id → quantity (summed over all bouquets)
     * @param names             display names used in error messages (keyed "p:id" / "s:id")
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(Map<Long, Integer> productQuantities, Map<Long, Integer> stemQuantities, Map<String, String> names) {
        for (Map.Entry<Long, Integer> e : new TreeMap<>(productQuantities).entrySet()) {
            if (productRepository.decrementStock(e.getKey(), e.getValue()) == 0) {
                Integer available = productRepository.findStockById(e.getKey());
                throw new InsufficientStockException(names.getOrDefault("p:" + e.getKey(), "Un article"),
                        available == null ? 0 : available);
            }
        }
        for (Map.Entry<Long, Integer> e : new TreeMap<>(stemQuantities).entrySet()) {
            if (stemRepository.decrementStock(e.getKey(), e.getValue()) == 0) {
                Integer available = stemRepository.findStockById(e.getKey());
                throw new InsufficientStockException(names.getOrDefault("s:" + e.getKey(), "Une fleur de votre bouquet"),
                        available == null ? 0 : available);
            }
        }
    }

    /** Returns the stock held by a cancelled order. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(CustomerOrder order) {
        Map<Long, Integer> products = new TreeMap<>();
        Map<Long, Integer> stems = new TreeMap<>();
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                products.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
            }
            BouquetSnapshot snapshot = item.getConfigurationSnapshot();
            if (snapshot != null) {
                for (BouquetSnapshot.StemLine line : snapshot.stems()) {
                    if (line.stemId() != null) {
                        stems.merge(line.stemId(), line.quantity() * item.getQuantity(), Integer::sum);
                    }
                }
            }
        }
        products.forEach(productRepository::restoreStock);
        stems.forEach(stemRepository::restoreStock);
    }
}
