package ma.bloome.service;

import ma.bloome.mapper.CatalogMapper.Rating;
import ma.bloome.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Aggregates review ratings in a single query per page of products. */
@Service
public class RatingService {

    private final ReviewRepository reviewRepository;

    public RatingService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Map<Long, Rating> ratingsFor(Collection<Long> productIds) {
        Map<Long, Rating> result = new HashMap<>();
        if (productIds.isEmpty()) {
            return result;
        }
        for (Object[] row : reviewRepository.ratingSummaries(productIds)) {
            result.put((Long) row[0], new Rating(round((Double) row[1]), (Long) row[2]));
        }
        return result;
    }

    public Rating ratingFor(Long productId) {
        List<Object[]> rows = reviewRepository.ratingSummary(productId);
        if (rows.isEmpty() || rows.get(0)[0] == null) {
            return Rating.NONE;
        }
        return new Rating(round((Double) rows.get(0)[0]), (Long) rows.get(0)[1]);
    }

    private static Double round(Double value) {
        return value == null ? null : Math.round(value * 10.0) / 10.0;
    }
}
