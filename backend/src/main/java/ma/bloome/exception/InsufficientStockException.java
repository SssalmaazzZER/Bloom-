package ma.bloome.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends ApiException {

    private final String itemName;
    private final int available;

    public InsufficientStockException(String itemName, int available) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", available <= 0
                ? "« " + itemName + " » n'est plus disponible."
                : "Il ne reste que " + available + " exemplaire(s) de « " + itemName + " ».");
        this.itemName = itemName;
        this.available = available;
    }

    public String getItemName() {
        return itemName;
    }

    public int getAvailable() {
        return available;
    }
}
