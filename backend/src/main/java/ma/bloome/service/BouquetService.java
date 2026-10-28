package ma.bloome.service;

import ma.bloome.config.BloomeProperties;
import ma.bloome.dto.BouquetDtos.*;
import ma.bloome.dto.CartDtos.BouquetRequest;
import ma.bloome.dto.CartDtos.BouquetStemLine;
import ma.bloome.dto.CartDtos.BouquetSummary;
import ma.bloome.dto.CartDtos.StemSelection;
import ma.bloome.entity.*;
import ma.bloome.exception.ApiException;
import ma.bloome.repository.BouquetConfigurationRepository;
import ma.bloome.repository.BouquetRibbonRepository;
import ma.bloome.repository.BouquetStemRepository;
import ma.bloome.repository.BouquetWrappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Validates and prices "Build your bouquet" configurations. Prices always come from the
 * database; the client only sends ids and quantities.
 */
@Service
public class BouquetService {

    /** A custom bouquet can be ordered at most this many times in one cart line. */
    public static final int MAX_BOUQUETS_PER_LINE = 5;

    private final BouquetStemRepository stemRepository;
    private final BouquetWrappingRepository wrappingRepository;
    private final BouquetRibbonRepository ribbonRepository;
    private final BouquetConfigurationRepository configurationRepository;
    private final BloomeProperties.Bouquet rules;
    private final String currency;

    public BouquetService(BouquetStemRepository stemRepository, BouquetWrappingRepository wrappingRepository,
                          BouquetRibbonRepository ribbonRepository,
                          BouquetConfigurationRepository configurationRepository, BloomeProperties properties) {
        this.stemRepository = stemRepository;
        this.wrappingRepository = wrappingRepository;
        this.ribbonRepository = ribbonRepository;
        this.configurationRepository = configurationRepository;
        this.rules = properties.bouquet();
        this.currency = properties.shop().currency();
    }

    public record StemQuantity(BouquetStem stem, int quantity) {
    }

    /** A bouquet whose components have been loaded and priced from the database. */
    public record ResolvedBouquet(BouquetWrapping wrapping, BouquetRibbon ribbon, String message,
                                  List<StemQuantity> stems, BigDecimal unitPrice, boolean available,
                                  int maxBouquets) {
        public int stemCount() {
            return stems.stream().mapToInt(StemQuantity::quantity).sum();
        }
    }

    @Transactional(readOnly = true)
    public BouquetOptions options() {
        List<StemOption> stems = stemRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(s -> new StemOption(s.getId(), s.getName(), s.getColor().name(), s.getDescription(),
                        s.getImageUrl(), s.getUnitPrice(), s.getMaxPerBouquet(), s.getStockQuantity()))
                .toList();
        List<WrappingOption> wrappings = wrappingRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(w -> new WrappingOption(w.getId(), w.getName(), w.getDescription(), w.getSwatchColor(), w.getPrice()))
                .toList();
        List<RibbonOption> ribbons = ribbonRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(r -> new RibbonOption(r.getId(), r.getName(), r.getSwatchColor(), r.getPrice()))
                .toList();
        return new BouquetOptions(stems, wrappings, ribbons, rules.minStems(), rules.maxStems(), currency);
    }

    /** Validates a request strictly; throws a 400 explaining the first problem found. */
    public ResolvedBouquet resolve(BouquetRequest request) {
        BouquetWrapping wrapping = wrappingRepository.findById(request.wrappingId())
                .filter(BouquetWrapping::isActive)
                .orElseThrow(() -> ApiException.badRequest("INVALID_BOUQUET", "Cet emballage n'est plus disponible."));
        BouquetRibbon ribbon = ribbonRepository.findById(request.ribbonId())
                .filter(BouquetRibbon::isActive)
                .orElseThrow(() -> ApiException.badRequest("INVALID_BOUQUET", "Ce ruban n'est plus disponible."));

        Set<Long> seen = new HashSet<>();
        for (StemSelection s : request.stems()) {
            if (!seen.add(s.stemId())) {
                throw ApiException.badRequest("INVALID_BOUQUET", "Chaque fleur ne peut apparaître qu'une fois.");
            }
        }
        Map<Long, BouquetStem> stemsById = stemRepository.findAllById(seen).stream()
                .collect(Collectors.toMap(BouquetStem::getId, Function.identity()));

        List<StemQuantity> lines = new ArrayList<>();
        for (StemSelection selection : request.stems()) {
            BouquetStem stem = stemsById.get(selection.stemId());
            if (stem == null || !stem.isActive()) {
                throw ApiException.badRequest("INVALID_BOUQUET", "Une des fleurs choisies n'est plus disponible.");
            }
            if (selection.quantity() < 1 || selection.quantity() > stem.getMaxPerBouquet()) {
                throw ApiException.badRequest("INVALID_BOUQUET",
                        "Vous pouvez ajouter jusqu'à " + stem.getMaxPerBouquet() + " tiges de « " + stem.getName() + " ».");
            }
            lines.add(new StemQuantity(stem, selection.quantity()));
        }
        int total = lines.stream().mapToInt(StemQuantity::quantity).sum();
        if (total < rules.minStems() || total > rules.maxStems()) {
            throw ApiException.badRequest("INVALID_BOUQUET",
                    "Un bouquet compte entre " + rules.minStems() + " et " + rules.maxStems() + " tiges (actuellement " + total + ").");
        }
        String message = request.message() == null || request.message().isBlank() ? null : request.message().trim();
        return build(wrapping, ribbon, message, lines);
    }

    /** Re-prices a stored configuration with current data; never throws for availability. */
    public ResolvedBouquet resolve(BouquetConfiguration configuration) {
        List<StemQuantity> lines = configuration.getStems().stream()
                .sorted(Comparator.comparingInt(l -> l.getStem().getSortOrder()))
                .map(l -> new StemQuantity(l.getStem(), l.getQuantity()))
                .toList();
        return build(configuration.getWrapping(), configuration.getRibbon(), configuration.getMessage(), lines);
    }

    public BouquetConfiguration persist(ResolvedBouquet resolved, User owner) {
        BouquetConfiguration configuration = new BouquetConfiguration();
        configuration.setUser(owner);
        configuration.setWrapping(resolved.wrapping());
        configuration.setRibbon(resolved.ribbon());
        configuration.setMessage(resolved.message());
        configuration = configurationRepository.save(configuration);
        for (StemQuantity line : resolved.stems()) {
            BouquetConfigurationStem entry = new BouquetConfigurationStem();
            entry.setId(new BouquetConfigurationStem.Key(configuration.getId(), line.stem().getId()));
            entry.setConfiguration(configuration);
            entry.setStem(line.stem());
            entry.setQuantity(line.quantity());
            configuration.getStems().add(entry);
        }
        return configurationRepository.save(configuration);
    }

    public BouquetSnapshot snapshot(ResolvedBouquet resolved) {
        List<BouquetSnapshot.StemLine> stems = resolved.stems().stream()
                .map(s -> new BouquetSnapshot.StemLine(s.stem().getId(), s.stem().getName(), s.stem().getColor().name(),
                        s.stem().getUnitPrice(), s.quantity()))
                .toList();
        return new BouquetSnapshot(resolved.wrapping().getName(), resolved.wrapping().getPrice(),
                resolved.ribbon().getName(), resolved.ribbon().getPrice(), resolved.message(), stems);
    }

    public BouquetSummary summary(ResolvedBouquet resolved) {
        List<BouquetStemLine> stems = resolved.stems().stream()
                .map(s -> new BouquetStemLine(s.stem().getId(), s.stem().getName(), s.stem().getColor().name(),
                        s.stem().getImageUrl(), s.stem().getUnitPrice(), s.quantity(),
                        s.stem().getUnitPrice().multiply(BigDecimal.valueOf(s.quantity()))))
                .toList();
        BouquetWrapping w = resolved.wrapping();
        BouquetRibbon r = resolved.ribbon();
        return new BouquetSummary(w.getId(), w.getName(), w.getSwatchColor(), w.getPrice(),
                r.getId(), r.getName(), r.getSwatchColor(), r.getPrice(),
                resolved.message(), stems, resolved.stemCount(), resolved.unitPrice());
    }

    private ResolvedBouquet build(BouquetWrapping wrapping, BouquetRibbon ribbon, String message, List<StemQuantity> lines) {
        BigDecimal price = wrapping.getPrice().add(ribbon.getPrice());
        boolean available = wrapping.isActive() && ribbon.isActive() && !lines.isEmpty();
        int maxBouquets = MAX_BOUQUETS_PER_LINE;
        for (StemQuantity line : lines) {
            price = price.add(line.stem().getUnitPrice().multiply(BigDecimal.valueOf(line.quantity())));
            if (!line.stem().isActive()) {
                available = false;
            }
            maxBouquets = Math.min(maxBouquets, line.stem().getStockQuantity() / line.quantity());
        }
        return new ResolvedBouquet(wrapping, ribbon, message, lines, price.setScale(2, java.math.RoundingMode.HALF_UP),
                available, Math.max(0, maxBouquets));
    }
}
