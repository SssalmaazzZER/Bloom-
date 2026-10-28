package ma.bloome.repository;

import ma.bloome.entity.Occasion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OccasionRepository extends JpaRepository<Occasion, Long> {

    List<Occasion> findAllByOrderBySortOrderAscNameAsc();

    List<Occasion> findBySlugIn(Collection<String> slugs);
}
